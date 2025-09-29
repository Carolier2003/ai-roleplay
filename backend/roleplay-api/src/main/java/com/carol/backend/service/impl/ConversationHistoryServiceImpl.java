package com.carol.backend.service.impl;

import com.carol.backend.dto.ConversationMessageVO;
import com.carol.backend.dto.ChatHistoryResponse;
import com.carol.backend.service.IConversationHistoryService;
import com.carol.backend.service.CustomMessageStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 聊天历史服务实现 - 适配Spring AI Redis存储
 * 
 * @author carol
 */
@Slf4j
@Service
public class ConversationHistoryServiceImpl implements IConversationHistoryService {
    
    @Autowired
    private MessageWindowChatMemory messageWindowChatMemory;
    
    @Autowired
    private CustomMessageStorageService customMessageStorageService;
    
    // Redis中保留的最大消息数量（Spring AI MessageWindowChatMemory的限制）
    private static final int MAX_REDIS_MESSAGES = 100;
    
    // 历史记录查询的最大天数限制
    private static final int MAX_HISTORY_DAYS = 30;
    
    @Override
    public ChatHistoryResponse getChatHistory(Long characterId, Long userId) {
        String conversationId = generateConversationId(characterId, userId);
        
        log.info("[getChatHistory] 查询聊天历史: conversationId={}", conversationId);
        
        try {
            // 🎯 优先从自定义存储中获取消息（包含真实时间戳）
            List<CustomMessageStorageService.StoredMessage> customMessages = 
                customMessageStorageService.getMessages(conversationId);
            
            if (!customMessages.isEmpty()) {
                log.info("[getChatHistory] 从自定义存储获取到 {} 条消息", customMessages.size());
                List<ConversationMessageVO> messages = convertStoredMessagesToVO(customMessages, characterId);
                
                return new ChatHistoryResponse()
                    .setMessages(messages)
                    .setTotal(messages.size())
                    .setHasMore(false)
                    .setSourceStats(Map.of("custom_storage", (long) messages.size()))
                    .setQueryDays(7);
            }
            
            // 如果自定义存储为空，回退到Spring AI存储（兼容旧数据）
            List<Message> redisMessages = messageWindowChatMemory.get(conversationId);
            
            if (!redisMessages.isEmpty()) {
                log.info("[getChatHistory] 从Spring AI Redis获取到 {} 条消息", redisMessages.size());
                List<ConversationMessageVO> messages = convertSpringAIMessagesToVO(redisMessages, characterId);
                
                return new ChatHistoryResponse()
                    .setMessages(messages)
                    .setTotal(messages.size())
                    .setHasMore(false)
                    .setSourceStats(Map.of("spring_ai_redis", (long) messages.size()))
                    .setQueryDays(7);
            }
            
            // 都为空，返回空结果
            log.info("[getChatHistory] 所有存储都为空，返回空历史记录");
            
            return new ChatHistoryResponse()
                .setMessages(Collections.emptyList())
                .setTotal(0)
                .setHasMore(false)
                .setSourceStats(Map.of("empty", 0L))
                .setQueryDays(7);
            
        } catch (Exception e) {
            log.error("[getChatHistory] 查询聊天历史失败: {}", e.getMessage(), e);
            throw new RuntimeException("查询聊天历史失败: " + e.getMessage());
        }
    }
    
    @Override
    public ChatHistoryResponse getAllChatHistory(Long userId) {
        log.info("[getAllChatHistory] 查询用户所有角色的聊天历史: userId={}", userId);
        
        try {
            // 获取所有可能的角色ID（这里需要查询角色表，暂时硬编码几个常用角色）
            List<Long> characterIds = List.of(1L, 2L, 3L); // 哈利·波特、苏格拉底、爱因斯坦
            
            List<ConversationMessageVO> allMessages = new ArrayList<>();
            Map<String, Long> sourceStats = new HashMap<>();
            sourceStats.put("redis", 0L);
            
            // 遍历所有角色，获取各自的聊天历史
            for (Long characterId : characterIds) {
                String conversationId = generateConversationId(characterId, userId);
                
                try {
                    List<Message> redisMessages = messageWindowChatMemory.get(conversationId);
                    
                    if (!redisMessages.isEmpty()) {
                        List<ConversationMessageVO> characterMessages = convertSpringAIMessagesToVO(redisMessages, characterId);
                        allMessages.addAll(characterMessages);
                        sourceStats.put("redis", sourceStats.get("redis") + characterMessages.size());
                        
                        log.info("[getAllChatHistory] 角色 {} 的消息数量: {}", characterId, characterMessages.size());
                    }
                } catch (Exception e) {
                    log.warn("[getAllChatHistory] 获取角色 {} 的历史记录失败: {}", characterId, e.getMessage());
                }
            }
            
            // 按时间戳排序（最新的在前）
            allMessages.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
            
            log.info("[getAllChatHistory] 总共获取到 {} 条消息", allMessages.size());
            
            return new ChatHistoryResponse()
                .setMessages(allMessages)
                .setTotal(allMessages.size())
                .setHasMore(false)
                .setSourceStats(sourceStats)
                .setQueryDays(7);
            
        } catch (Exception e) {
            log.error("[getAllChatHistory] 查询所有聊天历史失败: {}", e.getMessage(), e);
            throw new RuntimeException("查询所有聊天历史失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<ConversationMessageVO> getArchivedHistory(Long characterId, Long userId, 
                                                         Long beforeTime, Integer limit) {
        // 暂时返回空列表，后续实现MySQL查询
        log.info("[getArchivedHistory] 暂未实现MySQL查询，返回空列表");
        return Collections.emptyList();
    }
    
    @Override
    public boolean deleteMessage(String messageId, Long characterId, Long userId) {
        // 暂时返回false，后续实现删除逻辑
        log.warn("[deleteMessage] 暂未实现删除逻辑: messageId={}", messageId);
        return false;
    }
    
    @Override
    public boolean clearConversation(Long characterId, Long userId) {
        String conversationId = generateConversationId(characterId, userId);
        
        try {
            log.info("[clearConversation] 开始清空对话: conversationId={}", conversationId);
            
            // 清空自定义存储中的对话
            customMessageStorageService.clearMessages(conversationId);
            log.info("[clearConversation] 自定义存储对话已清空");
            
            // 清空Redis中的对话（Spring AI支持）
            messageWindowChatMemory.clear(conversationId);
            log.info("[clearConversation] Spring AI Redis对话已清空");
            
            return true;
            
        } catch (Exception e) {
            log.error("[clearConversation] 清空对话失败: conversationId={}, error={}", conversationId, e.getMessage(), e);
            throw new RuntimeException("清空对话失败: " + e.getMessage());
        }
    }
    
    @Override
    public boolean clearAllConversations(Long userId) {
        log.info("[clearAllConversations] 开始清空用户所有对话: userId={}", userId);
        
        try {
            // 获取所有可能的角色ID（这里需要查询角色表，暂时硬编码几个常用角色）
            List<Long> characterIds = List.of(1L, 2L, 3L, 4L, 5L); // 扩展角色列表
            
            int clearedCount = 0;
            
            // 遍历所有角色，清空各自的对话
            for (Long characterId : characterIds) {
                try {
                    String conversationId = generateConversationId(characterId, userId);
                    
                    // 检查自定义存储是否有对话记录
                    List<CustomMessageStorageService.StoredMessage> customMessages = 
                        customMessageStorageService.getMessages(conversationId);
                    
                    // 检查Spring AI存储是否有对话记录
                    List<Message> messages = messageWindowChatMemory.get(conversationId);
                    
                    boolean hasCustomMessages = !customMessages.isEmpty();
                    boolean hasSpringAIMessages = !messages.isEmpty();
                    
                    if (hasCustomMessages || hasSpringAIMessages) {
                        // 清空自定义存储
                        if (hasCustomMessages) {
                            customMessageStorageService.clearMessages(conversationId);
                            log.info("[clearAllConversations] 已清空角色 {} 的自定义存储对话，消息数: {}", characterId, customMessages.size());
                        }
                        
                        // 清空Spring AI存储
                        if (hasSpringAIMessages) {
                            messageWindowChatMemory.clear(conversationId);
                            log.info("[clearAllConversations] 已清空角色 {} 的Spring AI对话，消息数: {}", characterId, messages.size());
                        }
                        
                        clearedCount++;
                    }
                } catch (Exception e) {
                    log.warn("[clearAllConversations] 清空角色 {} 的对话失败: {}", characterId, e.getMessage());
                }
            }
            
            // 同时清空通用对话（没有指定角色的对话）
            try {
                String generalConversationId = generateConversationId(null, userId);
                List<Message> generalMessages = messageWindowChatMemory.get(generalConversationId);
                if (!generalMessages.isEmpty()) {
                    messageWindowChatMemory.clear(generalConversationId);
                    clearedCount++;
                    log.info("[clearAllConversations] 已清空通用对话，消息数: {}", generalMessages.size());
                }
            } catch (Exception e) {
                log.warn("[clearAllConversations] 清空通用对话失败: {}", e.getMessage());
            }
            
            log.info("[clearAllConversations] 所有对话清空完成，清空了 {} 个对话", clearedCount);
            return true;
            
        } catch (Exception e) {
            log.error("[clearAllConversations] 清空所有对话失败: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("清空所有对话失败: " + e.getMessage());
        }
    }
    
    @Override
    public void archiveRedisDataToMySQL() {
        // 暂时只记录日志，后续实现归档逻辑
        log.info("[archiveRedisDataToMySQL] 暂未实现归档逻辑");
    }
    
    /**
     * 将自定义存储的消息转换为VO（包含真实时间戳）
     */
    private List<ConversationMessageVO> convertStoredMessagesToVO(
            List<CustomMessageStorageService.StoredMessage> storedMessages, Long characterId) {
        List<ConversationMessageVO> result = new ArrayList<>();
        
        for (int i = 0; i < storedMessages.size(); i++) {
            CustomMessageStorageService.StoredMessage storedMessage = storedMessages.get(i);
            ConversationMessageVO vo = new ConversationMessageVO();
            
            vo.setMessageId("stored_" + i + "_" + storedMessage.getTimestamp());
            vo.setCharacterId(characterId);
            vo.setContent(storedMessage.getContent());
            vo.setIsUser(storedMessage.getIsUser());
            vo.setMessageType(storedMessage.getIsUser() ? 1 : 2);
            
            // 🎯 使用真实的时间戳！
            vo.setTimestamp(storedMessage.getTimestamp());
            vo.setCreatedTime(storedMessage.getCreatedTime());
            
            // 设置音频URL和语音时长
            vo.setAudioUrl(storedMessage.getAudioUrl());
            vo.setVoiceDuration(storedMessage.getVoiceDuration());
            
            vo.setDataSource("custom_storage");
            vo.setNeedSync(false);
            
            result.add(vo);
        }
        
        log.info("[convertStoredMessagesToVO] 转换完成: {} 条消息，包含真实时间戳", result.size());
        return result;
    }
    
    /**
     * 将Spring AI的Message转换为VO
     */
    private List<ConversationMessageVO> convertSpringAIMessagesToVO(List<Message> messages, Long characterId) {
        List<ConversationMessageVO> result = new ArrayList<>();
        
        // 🎯 修复时间戳问题：使用更合理的时间间隔来模拟历史消息时间
        // 假设消息之间的平均间隔为2-5分钟，而不是1秒
        long currentTime = System.currentTimeMillis();
        
        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);
            ConversationMessageVO vo = new ConversationMessageVO();
            
            vo.setMessageId(generateMessageId(message, i));
            vo.setCharacterId(characterId);
            // 提取纯文本内容
            String content = extractMessageContent(message);
            vo.setContent(content);
            vo.setIsUser(message instanceof UserMessage);
            vo.setMessageType(message instanceof UserMessage ? 1 : 2);
            
            // 🎯 改进时间戳生成逻辑：
            // 1. 尝试从消息元数据中获取时间戳
            Long messageTimestamp = extractTimestampFromMessage(message);
            if (messageTimestamp != null) {
                vo.setTimestamp(messageTimestamp);
                vo.setCreatedTime(LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(messageTimestamp), 
                    java.time.ZoneId.systemDefault()
                ));
            } else {
                // 2. 如果没有时间戳，使用更合理的时间间隔（2-5分钟）
                int minutesAgo = (messages.size() - i) * (2 + (i % 4)); // 2-5分钟间隔
                long timestamp = currentTime - (minutesAgo * 60 * 1000L);
                vo.setTimestamp(timestamp);
                vo.setCreatedTime(LocalDateTime.now().minusMinutes(minutesAgo));
            }
            
            // 尝试从消息元数据中提取audioUrl
            String audioUrl = extractAudioUrlFromMessage(message);
            vo.setAudioUrl(audioUrl);
            
            vo.setDataSource("redis");
            vo.setNeedSync(false);
            
            result.add(vo);
        }
        
        return result;
    }
    
    /**
     * 尝试从Spring AI Message中提取audioUrl
     */
    private String extractAudioUrlFromMessage(Message message) {
        try {
            // 从消息元数据中提取audioUrl
            Map<String, Object> metadata = message.getMetadata();
            if (metadata != null && metadata.containsKey("audioUrl")) {
                Object audioUrlObj = metadata.get("audioUrl");
                if (audioUrlObj instanceof String) {
                    String audioUrl = (String) audioUrlObj;
                    log.debug("[extractAudioUrlFromMessage] 从消息元数据中提取到audioUrl: {}", audioUrl);
                    return audioUrl;
                }
            }
            
            // 如果元数据中没有audioUrl，返回null
            return null;
        } catch (Exception e) {
            log.warn("[extractAudioUrlFromMessage] 提取audioUrl失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 尝试从Spring AI Message中提取时间戳
     */
    private Long extractTimestampFromMessage(Message message) {
        try {
            // 尝试从消息的元数据中获取时间戳
            if (message.getMetadata() != null) {
                Object timestamp = message.getMetadata().get("timestamp");
                if (timestamp instanceof Long) {
                    return (Long) timestamp;
                } else if (timestamp instanceof String) {
                    return Long.parseLong((String) timestamp);
                }
                
                // 尝试其他可能的时间戳字段名
                String[] timestampFields = {"createdAt", "created_at", "messageTime", "time"};
                for (String field : timestampFields) {
                    Object value = message.getMetadata().get(field);
                    if (value instanceof Long) {
                        return (Long) value;
                    } else if (value instanceof String) {
                        try {
                            return Long.parseLong((String) value);
                        } catch (NumberFormatException ignored) {
                            // 继续尝试下一个字段
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[extractTimestampFromMessage] 提取时间戳失败: {}", e.getMessage());
        }
        
        return null; // 没有找到有效的时间戳
    }
    
    /**
     * 生成会话ID - 与ChatController保持一致
     */
    private String generateConversationId(Long characterId, Long userId) {
        if (characterId != null) {
            return String.format("user_%d_char_%d", userId, characterId);
        } else {
            return String.format("user_%d_general", userId);
        }
    }
    
    /**
     * 提取消息的纯文本内容
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
     * 生成消息ID
     */
    private String generateMessageId(Message message, int index) {
        // 基于消息内容和索引生成唯一ID
        String content = extractMessageContent(message);
        return String.format("msg_%d_%d", content.hashCode(), index);
    }
}
