package com.carol.backend.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.carol.backend.service.ICustomMessageStorageService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 自定义消息存储服务
 * 
 * @author jianjl
 * @version 1.0
 * @description 在Redis中保存消息内容和时间戳信息
 * @date 2025-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomMessageStorageService implements ICustomMessageStorageService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String MESSAGE_KEY_PREFIX = "chat:messages:";
    private static final int MESSAGE_TTL_DAYS = 30; // 消息保存30天
    
    @Override
    public void saveMessage(String conversationId, Message message, boolean isUser) {
        saveMessage(conversationId, message, isUser, null);
    }
    
    @Override
    public void updateMessageAudioUrl(String conversationId, String messageContent, String audioUrl) {
        updateMessageAudioInfo(conversationId, messageContent, audioUrl, null);
    }
    
    @Override
    public void updateMessageAudioInfo(String conversationId, String messageContent, String audioUrl, Integer voiceDuration) {
        try {
            String messageKey = MESSAGE_KEY_PREFIX + conversationId;
            List<String> messageJsonList = redisTemplate.opsForList().range(messageKey, 0, -1);
            
            if (messageJsonList != null) {
                for (int i = 0; i < messageJsonList.size(); i++) {
                    String messageJson = messageJsonList.get(i);
                    StoredMessage msg = objectMapper.readValue(messageJson, StoredMessage.class);
                    
                    // 找到匹配的AI消息（非用户消息且内容匹配）
                    if (!msg.getIsUser() && msg.getContent().equals(messageContent)) {
                        if (audioUrl != null) {
                            msg.setAudioUrl(audioUrl);
                        }
                        if (voiceDuration != null) {
                            msg.setVoiceDuration(voiceDuration);
                        }
                        // 更新Redis中的消息
                        String updatedJson = objectMapper.writeValueAsString(msg);
                        redisTemplate.opsForList().set(messageKey, i, updatedJson);
                        log.info("[updateMessageAudioInfo] 更新消息音频信息成功: conversationId={}, audioUrl={}, voiceDuration={}", 
                                conversationId, audioUrl, voiceDuration);
                        return;
                    }
                }
            }
            
            log.warn("[updateMessageAudioInfo] 未找到匹配的消息: conversationId={}, content={}", 
                    conversationId, messageContent.substring(0, Math.min(50, messageContent.length())));
            
        } catch (Exception e) {
            log.error("[updateMessageAudioInfo] 更新消息音频信息失败: conversationId={}, error={}", 
                    conversationId, e.getMessage(), e);
        }
    }
    
    @Override
    public boolean updateUserMessageVoiceDuration(String conversationId, String messageContent, Integer voiceDuration) {
        try {
            String messageKey = MESSAGE_KEY_PREFIX + conversationId;
            List<String> messageJsonList = redisTemplate.opsForList().range(messageKey, 0, -1);
            
            if (messageJsonList != null) {
                // 从最后一条消息开始查找，因为用户消息通常是最近的
                for (int i = messageJsonList.size() - 1; i >= 0; i--) {
                    String messageJson = messageJsonList.get(i);
                    StoredMessage msg = objectMapper.readValue(messageJson, StoredMessage.class);
                    
                    // 找到匹配的用户消息（用户消息且内容匹配）
                    if (Boolean.TRUE.equals(msg.getIsUser()) && msg.getContent().equals(messageContent)) {
                        msg.setVoiceDuration(voiceDuration);
                        
                        // 更新Redis中的消息
                        String updatedJson = objectMapper.writeValueAsString(msg);
                        redisTemplate.opsForList().set(messageKey, i, updatedJson);
                        
                        log.info("[updateUserMessageVoiceDuration] 更新用户消息语音时长成功: conversationId={}, voiceDuration={}, content={}", 
                                conversationId, voiceDuration, messageContent.substring(0, Math.min(50, messageContent.length())));
                        return true;
                    }
                }
            }
            
            log.warn("[updateUserMessageVoiceDuration] 未找到匹配的用户消息: conversationId={}, content={}", 
                    conversationId, messageContent.substring(0, Math.min(50, messageContent.length())));
            return false;
            
        } catch (Exception e) {
            log.error("[updateUserMessageVoiceDuration] 更新用户消息语音时长失败: conversationId={}, error={}", 
                    conversationId, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public void saveMessage(String conversationId, Message message, boolean isUser, String audioUrl) {
        saveMessage(conversationId, message, isUser, audioUrl, null);
    }
    
    @Override
    public void saveMessage(String conversationId, Message message, boolean isUser, String audioUrl, Integer voiceDuration) {
        try {
            String messageKey = MESSAGE_KEY_PREFIX + conversationId;
            
            // 创建消息存储对象
            StoredMessage storedMessage = new StoredMessage();
            storedMessage.setContent(extractMessageContent(message));
            storedMessage.setIsUser(isUser);
            storedMessage.setTimestamp(System.currentTimeMillis());
            storedMessage.setCreatedTime(LocalDateTime.now());
            storedMessage.setMessageType(isUser ? "USER" : "ASSISTANT");
            storedMessage.setAudioUrl(audioUrl);
            storedMessage.setVoiceDuration(voiceDuration);
            
            // 序列化为JSON
            String messageJson = objectMapper.writeValueAsString(storedMessage);
            
            // 添加到Redis列表
            redisTemplate.opsForList().rightPush(messageKey, messageJson);
            
            // 设置过期时间
            redisTemplate.expire(messageKey, MESSAGE_TTL_DAYS, TimeUnit.DAYS);
            
            log.info("[saveMessage] 消息保存成功: conversationId={}, isUser={}, timestamp={}", 
                    conversationId, isUser, storedMessage.getTimestamp());
            
        } catch (JsonProcessingException e) {
            log.error("[saveMessage] 序列化消息失败: conversationId={}, error={}", 
                    conversationId, e.getMessage(), e);
        }
    }
    
    @Override
    public List<StoredMessage> getMessages(String conversationId) {
        try {
            String messageKey = MESSAGE_KEY_PREFIX + conversationId;
            List<String> messageJsonList = redisTemplate.opsForList().range(messageKey, 0, -1);
            
            List<StoredMessage> messages = new ArrayList<>();
            if (messageJsonList != null) {
                for (String messageJson : messageJsonList) {
                    try {
                        StoredMessage message = objectMapper.readValue(messageJson, StoredMessage.class);
                        messages.add(message);
                    } catch (JsonProcessingException e) {
                        log.warn("[getMessages] 反序列化消息失败: error={}", e.getMessage());
                    }
                }
            }
            
            log.info("[getMessages] 获取消息历史成功: conversationId={}, count={}", 
                    conversationId, messages.size());
            return messages;
            
        } catch (Exception e) {
            log.error("[getMessages] 获取消息历史失败: conversationId={}, error={}", 
                    conversationId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public void clearMessages(String conversationId) {
        log.info("[clearMessages] 清空会话消息: conversationId={}", conversationId);
        
        try {
            String messageKey = MESSAGE_KEY_PREFIX + conversationId;
            redisTemplate.delete(messageKey);
            log.info("[clearMessages] 清空消息成功: conversationId={}", conversationId);
        } catch (Exception e) {
            log.error("[clearMessages] 清空消息失败: conversationId={}, error={}", 
                    conversationId, e.getMessage(), e);
        }
    }
    
    /**
     * 提取消息内容
     */
    private String extractMessageContent(Message message) {
        // 尝试多种可能的方法名
        String[] methodNames = {"getContent", "content", "getText", "getTextContent", "textContent"};
        
        for (String methodName : methodNames) {
            try {
                java.lang.reflect.Method method = message.getClass().getMethod(methodName);
                Object content = method.invoke(message);
                if (content != null) {
                    String contentStr = content.toString();
                    // 如果内容不是完整的对象字符串表示，则返回
                    if (!contentStr.contains("Message [") && !contentStr.contains("messageType=")) {
                        return contentStr;
                    }
                }
            } catch (Exception e) {
                // 继续尝试下一个方法
            }
        }
        
        // 如果所有方法都失败，尝试从toString()中提取内容
        String fullString = message.toString();
        
        // 尝试从AssistantMessage的toString()中提取textContent
        if (fullString.contains("textContent=")) {
            int start = fullString.indexOf("textContent=") + "textContent=".length();
            int end = fullString.indexOf(", metadata=");
            if (end == -1) {
                end = fullString.indexOf("]", start);
            }
            if (start < fullString.length() && end > start) {
                return fullString.substring(start, end).trim();
            }
        }
        
        // 尝试从UserMessage的toString()中提取content
        if (fullString.contains("content='")) {
            int start = fullString.indexOf("content='") + "content='".length();
            int end = fullString.indexOf("'", start);
            if (start < fullString.length() && end > start) {
                return fullString.substring(start, end);
            }
        }
        
        log.warn("[extractMessageContent] 无法提取消息内容，返回原始字符串: messageType={}", 
                message.getClass().getSimpleName());
        return fullString;
    }
    
    /**
     * 存储的消息对象
     */
    @Data
    public static class StoredMessage {
        /**
         * 消息内容
         */
        private String content;
        
        /**
         * 是否为用户消息
         */
        private Boolean isUser;
        
        /**
         * 时间戳（毫秒）
         */
        private Long timestamp;
        
        /**
         * 创建时间
         */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdTime;
        
        /**
         * 消息类型
         */
        private String messageType;
        
        /**
         * 音频URL
         */
        private String audioUrl;
        
        /**
         * 语音时长（秒）
         */
        private Integer voiceDuration;
    }
}
