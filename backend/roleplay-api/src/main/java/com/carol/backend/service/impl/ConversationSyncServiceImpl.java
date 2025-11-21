package com.carol.backend.service.impl;

import com.carol.backend.entity.Conversation;
import com.carol.backend.entity.ConversationMessage;
import com.carol.backend.enums.ErrorCode;
import com.carol.backend.exception.BusinessException;
import com.carol.backend.mapper.ConversationMapper;
import com.carol.backend.mapper.ConversationMessageMapper;
import com.carol.backend.service.IConversationSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 对话同步服务实现类
 * 
 * @author jianjl
 * @version 1.0
 * @description 直接从Redis读取数据同步到MySQL数据仓库
 * @date 2025-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationSyncServiceImpl implements IConversationSyncService {
    
    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper conversationMessageMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String REDIS_CHAT_MEMORY_PREFIX = "spring_ai_alibaba_chat_memory:";
    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("user_(.+?)_(?:char_(\\d+)|general)");
    
    @Override
    public boolean syncConversation(String sessionId) {
        log.info("[syncConversation] 开始同步会话: sessionId={}", sessionId);
        
        try {
            // 步骤1: 检查是否已经同步过
            log.debug("[syncConversation] 检查会话是否已同步: sessionId={}", sessionId);
            int existingCount = 0;
            try {
                existingCount = conversationMapper.countBySessionId(sessionId);
                log.debug("[syncConversation] 数据库查询成功: existingCount={}", existingCount);
            } catch (Exception e) {
                log.error("[syncConversation] 数据库查询失败: sessionId={}, error={}", 
                        sessionId, e.getMessage(), e);
                throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "数据库查询失败", e);
            }
            
            if (existingCount > 0) {
                log.info("[syncConversation] 会话已同步过，跳过: sessionId={}, existingCount={}", 
                        sessionId, existingCount);
                return true;
            }
            
            // 步骤2: 从Redis获取对话数据
            log.debug("[syncConversation] 从Redis获取对话数据: sessionId={}", sessionId);
            String redisKey = REDIS_CHAT_MEMORY_PREFIX + sessionId;
            
            List<String> messageStrings = null;
            try {
                messageStrings = stringRedisTemplate.opsForList().range(redisKey, 0, -1);
                log.debug("[syncConversation] Redis读取完成: messageCount={}", 
                        messageStrings != null ? messageStrings.size() : 0);
            } catch (Exception e) {
                log.error("[syncConversation] Redis读取失败: sessionId={}, redisKey={}, error={}", 
                        sessionId, redisKey, e.getMessage(), e);
                throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "Redis读取失败", e);
            }
            
            if (messageStrings == null || messageStrings.isEmpty()) {
                log.warn("[syncConversation] Redis中没有找到消息: sessionId={}, redisKey={}", 
                        sessionId, redisKey);
                return false;
            }
            
            log.info("[syncConversation] 从Redis成功获取消息: sessionId={}, messageCount={}", 
                    sessionId, messageStrings.size());
            
            // 步骤3: 解析会话信息
            log.debug("[syncConversation] 解析会话信息: sessionId={}", sessionId);
            ConversationInfo conversationInfo = null;
            try {
                conversationInfo = parseSessionId(sessionId);
                if (conversationInfo == null) {
                    log.error("[syncConversation] 无法解析会话ID: sessionId={}", sessionId);
                    return false;
                }
                log.debug("[syncConversation] 解析会话信息成功: userId={}, characterId={}", 
                    conversationInfo.userId, conversationInfo.characterId);
            } catch (Exception e) {
                log.error("[syncConversation] 解析会话ID时发生异常: sessionId={}, error={}", 
                        sessionId, e.getMessage(), e);
                return false;
            }
            
            // 步骤4: 创建会话记录
            log.debug("[syncConversation] 构建会话记录: sessionId={}", sessionId);
            Conversation conversation = null;
            try {
                conversation = buildConversation(sessionId, messageStrings, conversationInfo);
                log.debug("[syncConversation] 会话记录构建完成: title={}, userId={}, characterId={}, messageCount={}", 
                    conversation.getTitle(), conversation.getUserId(), 
                    conversation.getCharacterId(), conversation.getMessageCount());
                    
                conversationMapper.insert(conversation);
                log.info("[syncConversation] 会话记录插入成功: conversationId={}, sessionId={}", 
                        conversation.getId(), sessionId);
                
            } catch (Exception e) {
                log.error("[syncConversation] 创建或插入会话记录失败: sessionId={}, error={}", 
                        sessionId, e.getMessage(), e);
                throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "创建会话记录失败", e);
            }
            
            // 步骤5: 创建消息记录
            log.debug("[syncConversation] 构建消息记录: sessionId={}", sessionId);
            List<ConversationMessage> conversationMessages = null;
            try {
                conversationMessages = buildConversationMessages(
                    conversation.getId(), sessionId, messageStrings);
                log.info("构建了 {} 条消息记录", conversationMessages.size());
                
                if (!conversationMessages.isEmpty()) {
                    for (int i = 0; i < conversationMessages.size(); i++) {
                        ConversationMessage message = conversationMessages.get(i);
                        log.info("插入消息 {}/{}: type={}, contentLength={}, content={}", 
                            i+1, conversationMessages.size(), 
                            message.getMessageType(), 
                            message.getContentLength(),
                            message.getContent().substring(0, Math.min(50, message.getContent().length())) + "...");
                            
                        conversationMessageMapper.insert(message);
                        log.info("消息 {} 插入成功，ID: {}", i+1, message.getId());
                    }
                }
            } catch (Exception e) {
                log.error("[syncConversation] 创建或插入消息记录失败: sessionId={}, error={}", 
                        sessionId, e.getMessage(), e);
                throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "创建消息记录失败", e);
            }
            
            // 步骤6: 更新同步状态
            log.debug("[syncConversation] 更新同步状态: sessionId={}", sessionId);
            try {
                conversation.setLastSyncAt(java.time.LocalDateTime.now());
                conversation.setSyncStatus(1);
                conversationMapper.updateById(conversation);
                log.debug("[syncConversation] 同步状态更新成功: sessionId={}", sessionId);
            } catch (Exception e) {
                log.error("[syncConversation] 更新同步状态失败: sessionId={}, error={}", 
                        sessionId, e.getMessage(), e);
                // 不抛出异常，因为主要数据已经插入成功
            }
            
            log.info("[syncConversation] 同步会话成功: sessionId={}, messageCount={}", 
                    sessionId, conversationMessages.size());
            return true;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[syncConversation] 同步会话失败: sessionId={}, error={}", 
                    sessionId, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public Map<String, Object> syncAllConversations() {
        log.info("[syncAllConversations] 开始批量同步所有Redis中的对话");
        
        Set<String> redisKeys = stringRedisTemplate.keys(REDIS_CHAT_MEMORY_PREFIX + "*");
        if (redisKeys == null || redisKeys.isEmpty()) {
            log.info("[syncAllConversations] Redis中没有找到对话数据");
            return Map.of("total", 0, "success", 0, "failed", 0);
        }
        
        int total = redisKeys.size();
        int success = 0;
        int failed = 0;
        
        log.info("[syncAllConversations] 找到对话数量: total={}", total);
        
        for (String redisKey : redisKeys) {
            String sessionId = redisKey.replace(REDIS_CHAT_MEMORY_PREFIX, "");
            
            try {
                if (syncConversation(sessionId)) {
                    success++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                log.error("[syncAllConversations] 同步会话时出错: sessionId={}, error={}", 
                        sessionId, e.getMessage(), e);
                failed++;
            }
        }
        
        log.info("[syncAllConversations] 批量同步完成: total={}, success={}, failed={}", total, success, failed);
        return Map.of(
            "total", total,
            "success", success,
            "failed", failed,
            "success_rate", total > 0 ? (double) success / total * 100 : 0
        );
    }
    
    /**
     * 解析会话ID，提取用户ID和角色ID
     */
    private ConversationInfo parseSessionId(String sessionId) {
        java.util.regex.Matcher matcher = SESSION_ID_PATTERN.matcher(sessionId);
        if (matcher.matches()) {
            String userId = matcher.group(1);
            String characterIdStr = matcher.group(2);
            Long characterId = characterIdStr != null ? Long.valueOf(characterIdStr) : null;
            
            return new ConversationInfo(userId, characterId);
        }
        return null;
    }
    
    /**
     * 构建对话记录
     */
    private Conversation buildConversation(String sessionId, List<String> messageStrings, 
                                         ConversationInfo info) {
        
        log.debug("[buildConversation] 开始构建对话记录: sessionId={}, messageCount={}, userId={}, characterId={}", 
            sessionId, messageStrings.size(), info.userId, info.characterId);
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minusMinutes(30); // 估算开始时间
        
        // 生成对话标题
        String title = generateConversationTitle(messageStrings, info.characterId);
        log.debug("[buildConversation] 生成标题: title={}", title);
        
        // 计算统计信息
        int messageCount = messageStrings.size();
        int totalTokens = estimateTokenCount(messageStrings);
        log.debug("[buildConversation] 统计信息: messageCount={}, totalTokens={}", messageCount, totalTokens);
        
        // 生成摘要
        String summary = generateConversationSummary(messageStrings);
        log.debug("[buildConversation] 生成摘要: summaryLength={}", summary != null ? summary.length() : 0);
        
        Conversation conversation = new Conversation()
            .setSessionId(sessionId)
            .setCharacterId(info.characterId)
            .setUserId(info.userId)
            .setTitle(title)
            .setContextSummary(summary)
            .setMessageCount(messageCount)
            .setTotalTokens(totalTokens)
            .setStartTime(startTime)
            .setEndTime(now)
            .setDurationMinutes(30)
            .setSyncSource("redis")
            .setSyncStatus(1)
            .setExportCount(0);
            
        log.debug("[buildConversation] 对话记录构建完成: sessionId={}", sessionId);
        return conversation;
    }
    
    /**
     * 构建对话消息列表
     */
    private List<ConversationMessage> buildConversationMessages(Long conversationId, 
                                                               String sessionId, 
                                                               List<String> messageStrings) {
        log.debug("[buildConversationMessages] 开始构建消息记录列表: messageCount={}", messageStrings.size());
        log.info("参数: conversationId={}, sessionId='{}', messageCount={}", 
            conversationId, sessionId, messageStrings.size());
        
        List<ConversationMessage> result = new ArrayList<>();
        
        for (int i = 0; i < messageStrings.size(); i++) {
            try {
                log.info("处理消息 {}/{}", i+1, messageStrings.size());
                String messageJson = messageStrings.get(i);
                log.info("原始JSON: {}", messageJson.substring(0, Math.min(200, messageJson.length())));
                
                JsonNode messageNode = objectMapper.readTree(messageJson);
                log.info("JSON解析成功");
                
                String messageType = messageNode.get("messageType").asText();
                String content = messageNode.get("text").asText();
                log.info("提取字段: messageType='{}', contentLength={}", messageType, content.length());
                
                int convertedMessageType = convertMessageType(messageType);
                log.info("消息类型转换: '{}' -> {}", messageType, convertedMessageType);
                
                ConversationMessage conversationMessage = new ConversationMessage()
                    .setConversationId(conversationId)
                    .setSessionId(sessionId)
                    .setMessageIndex(i + 1)
                    .setMessageType(convertedMessageType)
                    .setContent(content)
                    .setContentLength(content != null ? content.length() : 0)
                    .setTokenCount(estimateTokenCount(content))
                    .setLanguage("zh")
                    .setSyncSource("redis")
                    .setMessageTimestamp(LocalDateTime.now().minusMinutes(messageStrings.size() - i));
                
                // 如果是AI消息，可以添加更多元数据
                if ("ASSISTANT".equals(messageType)) {
                    conversationMessage.setModelName("qwen-plus");
                    conversationMessage.setRagKnowledgeUsed(false); // 可以后续优化
                    log.info("设置AI消息元数据: modelName='qwen-plus'");
                }
                
                result.add(conversationMessage);
                log.info("消息 {} 构建成功", i+1);
                
            } catch (Exception e) {
                log.error("[buildConversationMessages] 解析消息失败: index={}, error={}", i, e.getMessage(), e);
                log.error("问题消息内容: {}", messageStrings.get(i));
                // 继续处理下一条消息，不中断整个流程
            }
        }
        
        log.info("【buildConversationMessages】消息记录列表构建完成，成功构建{}条", result.size());
        return result;
    }
    
    /**
     * 转换消息类型
     */
    private Integer convertMessageType(String messageType) {
        switch (messageType) {
            case "USER":
                return ConversationMessage.MessageType.USER.getCode();
            case "ASSISTANT":
                return ConversationMessage.MessageType.AI.getCode();
            case "SYSTEM":
                return ConversationMessage.MessageType.SYSTEM.getCode();
            default:
                return ConversationMessage.MessageType.SYSTEM.getCode();
        }
    }
    
    /**
     * 生成对话标题
     */
    private String generateConversationTitle(List<String> messageStrings, Long characterId) {
        if (messageStrings.isEmpty()) {
            return "空对话";
        }
        
        try {
            // 查找第一个用户消息作为标题基础
            for (String messageJson : messageStrings) {
                JsonNode messageNode = objectMapper.readTree(messageJson);
                if ("USER".equals(messageNode.get("messageType").asText())) {
                    String content = messageNode.get("text").asText();
                    String title = content.length() > 20 ? 
                        content.substring(0, 20) + "..." : content;
                    return title;
                }
            }
        } catch (Exception e) {
            log.warn("生成对话标题失败: {}", e.getMessage());
        }
        
        return "对话记录";
    }
    
    /**
     * 生成对话摘要
     */
    private String generateConversationSummary(List<String> messageStrings) {
        if (messageStrings.size() <= 2) {
            return "简短对话";
        }
        
        int userMessageCount = 0;
        int aiMessageCount = 0;
        
        try {
            for (String messageJson : messageStrings) {
                JsonNode messageNode = objectMapper.readTree(messageJson);
                String messageType = messageNode.get("messageType").asText();
                if ("USER".equals(messageType)) {
                    userMessageCount++;
                } else if ("ASSISTANT".equals(messageType)) {
                    aiMessageCount++;
                }
            }
        } catch (Exception e) {
            log.warn("生成对话摘要失败: {}", e.getMessage());
        }
        
        return String.format("包含%d轮对话，用户消息%d条，AI回复%d条", 
            Math.min(userMessageCount, aiMessageCount), userMessageCount, aiMessageCount);
    }
    
    /**
     * 估算Token数量
     */
    private int estimateTokenCount(List<String> messageStrings) {
        int total = 0;
        try {
            for (String messageJson : messageStrings) {
                JsonNode messageNode = objectMapper.readTree(messageJson);
                String content = messageNode.get("text").asText();
                total += estimateTokenCount(content);
            }
        } catch (Exception e) {
            log.warn("估算Token失败: {}", e.getMessage());
        }
        return total;
    }
    
    /**
     * 估算单个文本的Token数量
     */
    private int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 简单估算：中文1个字=1个token，英文1个词=1个token
        return (int) (text.length() * 1.2); // 添加一些余量
    }
    
    /**
     * 会话信息类
     */
    private static class ConversationInfo {
        final String userId;
        final Long characterId;
        
        ConversationInfo(String userId, Long characterId) {
            this.userId = userId;
            this.characterId = characterId;
        }
    }
}