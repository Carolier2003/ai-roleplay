package com.carol.backend.service;

import com.carol.backend.entity.Conversation;
import com.carol.backend.entity.ConversationMessage;
import com.carol.backend.mapper.ConversationMapper;
import com.carol.backend.mapper.ConversationMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 对话同步服务（简化版）
 * 直接从Redis读取数据同步到MySQL数据仓库
 * 
 * @author carol
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationSyncService {
    
    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper conversationMessageMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String REDIS_CHAT_MEMORY_PREFIX = "spring_ai_alibaba_chat_memory:";
    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("user_(.+?)_(?:char_(\\d+)|general)");
    
    /**
     * 同步指定会话的对话数据
     */
    public boolean syncConversation(String sessionId) {
        log.info("=== 开始同步会话调试 ===");
        log.info("目标会话ID: {}", sessionId);
        log.info("当前时间: {}", java.time.LocalDateTime.now());
        
        try {
            // 步骤1: 检查是否已经同步过
            log.info("【步骤1】检查会话是否已同步: {}", sessionId);
            int existingCount = 0;
            try {
                log.info("正在执行SQL查询: SELECT COUNT(*) FROM conversations WHERE session_id = '{}'", sessionId);
                existingCount = conversationMapper.countBySessionId(sessionId);
                log.info("数据库查询成功: 现有记录数 = {}", existingCount);
            } catch (Exception e) {
                log.error("【ERROR】数据库查询失败", e);
                log.error("异常类型: {}", e.getClass().getName());
                log.error("异常消息: {}", e.getMessage());
                if (e.getCause() != null) {
                    log.error("根本原因: {}", e.getCause().getMessage());
                }
                throw new RuntimeException("数据库查询失败: " + e.getMessage(), e);
            }
            
            if (existingCount > 0) {
                log.info("【结果】会话 {} 已经同步过，记录数: {}，跳过同步", sessionId, existingCount);
                return true;
            }
            
            log.info("【步骤1完成】会话尚未同步，继续后续步骤");
            
            // 步骤2: 从Redis获取对话数据
            log.info("【步骤2】从Redis获取对话数据");
            String redisKey = REDIS_CHAT_MEMORY_PREFIX + sessionId;
            log.info("构建Redis Key: {}", redisKey);
            log.info("使用StringRedisTemplate进行读取...");
            
            List<String> messageStrings = null;
            try {
                log.info("执行Redis命令: LRANGE {} 0 -1", redisKey);
                messageStrings = stringRedisTemplate.opsForList().range(redisKey, 0, -1);
                log.info("Redis读取完成，原始结果类型: {}", messageStrings != null ? messageStrings.getClass().getName() : "null");
                log.info("Redis读取结果: {} 条记录", messageStrings != null ? messageStrings.size() : "null");
                
                if (messageStrings != null) {
                    log.info("消息列表详情:");
                    for (int i = 0; i < Math.min(messageStrings.size(), 3); i++) {
                        String msg = messageStrings.get(i);
                        log.info("  消息[{}]: 长度={}, 前100字符={}", i, msg.length(), 
                            msg.substring(0, Math.min(100, msg.length())));
                    }
                }
            } catch (Exception e) {
                log.error("【ERROR】Redis读取失败", e);
                log.error("异常类型: {}", e.getClass().getName());
                log.error("异常消息: {}", e.getMessage());
                log.error("Redis Key: {}", redisKey);
                if (e.getCause() != null) {
                    log.error("根本原因: {}", e.getCause().getMessage());
                }
                throw new RuntimeException("Redis读取失败: " + e.getMessage(), e);
            }
            
            if (messageStrings == null || messageStrings.isEmpty()) {
                log.warn("【结果】会话 {} 在Redis中没有找到消息", sessionId);
                log.warn("Redis Key: {}", redisKey);
                log.warn("messageStrings is null: {}", messageStrings == null);
                log.warn("messageStrings is empty: {}", messageStrings != null && messageStrings.isEmpty());
                return false;
            }
            
            log.info("【步骤2完成】从Redis成功获取到 {} 条消息", messageStrings.size());
            
            // 步骤3: 解析会话信息
            log.info("【步骤3】解析会话信息");
            log.info("目标会话ID: {}", sessionId);
            log.info("使用正则表达式: {}", SESSION_ID_PATTERN.pattern());
            
            ConversationInfo conversationInfo = null;
            try {
                conversationInfo = parseSessionId(sessionId);
                if (conversationInfo == null) {
                    log.error("【ERROR】无法解析会话ID: {}", sessionId);
                    log.error("会话ID格式不匹配正则表达式: {}", SESSION_ID_PATTERN.pattern());
                    return false;
                }
                log.info("解析会话信息成功: userId='{}', characterId={}", 
                    conversationInfo.userId, conversationInfo.characterId);
            } catch (Exception e) {
                log.error("【ERROR】解析会话ID时发生异常", e);
                return false;
            }
            
            log.info("【步骤3完成】会话信息解析成功");
            
            // 步骤4: 创建会话记录
            log.info("【步骤4】构建会话记录");
            Conversation conversation = null;
            try {
                conversation = buildConversation(sessionId, messageStrings, conversationInfo);
                log.info("会话记录构建完成: title='{}', userId='{}', characterId={}, messageCount={}", 
                    conversation.getTitle(), conversation.getUserId(), 
                    conversation.getCharacterId(), conversation.getMessageCount());
                    
                log.info("准备插入会话记录到数据库...");
                conversationMapper.insert(conversation);
                log.info("会话记录插入成功，生成的ID: {}", conversation.getId());
                
            } catch (Exception e) {
                log.error("【ERROR】创建或插入会话记录失败", e);
                log.error("异常类型: {}", e.getClass().getName());
                log.error("异常消息: {}", e.getMessage());
                if (e.getCause() != null) {
                    log.error("根本原因: {}", e.getCause().getMessage());
                }
                throw new RuntimeException("创建会话记录失败: " + e.getMessage(), e);
            }
            
            log.info("【步骤4完成】会话记录创建成功");
            
            // 步骤5: 创建消息记录
            log.info("【步骤5】构建消息记录");
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
                log.error("【ERROR】创建或插入消息记录失败", e);
                log.error("异常类型: {}", e.getClass().getName());
                log.error("异常消息: {}", e.getMessage());
                if (e.getCause() != null) {
                    log.error("根本原因: {}", e.getCause().getMessage());
                }
                throw new RuntimeException("创建消息记录失败: " + e.getMessage(), e);
            }
            
            log.info("【步骤5完成】消息记录创建成功");
            
            // 步骤6: 更新同步状态
            log.info("【步骤6】更新同步状态");
            try {
                conversation.setLastSyncAt(java.time.LocalDateTime.now());
                conversation.setSyncStatus(1);
                conversationMapper.updateById(conversation);
                log.info("同步状态更新成功");
            } catch (Exception e) {
                log.error("【ERROR】更新同步状态失败", e);
                // 不抛出异常，因为主要数据已经插入成功
            }
            
            log.info("【同步完成】成功同步会话: {}, 消息数: {}", sessionId, conversationMessages.size());
            log.info("=== 同步会话调试结束 ===");
            return true;
            
        } catch (Exception e) {
            log.error("【FINAL ERROR】同步会话最终失败: sessionId={}", sessionId);
            log.error("异常类型: {}", e.getClass().getName());
            log.error("异常消息: {}", e.getMessage());
            log.error("异常堆栈: ", e);
            if (e.getCause() != null) {
                log.error("根本原因类型: {}", e.getCause().getClass().getName());
                log.error("根本原因消息: {}", e.getCause().getMessage());
            }
            log.error("=== 同步会话调试异常结束 ===");
            return false;
        }
    }
    
    /**
     * 批量同步所有Redis中的对话
     */
    public Map<String, Object> syncAllConversations() {
        log.info("开始批量同步所有对话");
        
        Set<String> redisKeys = stringRedisTemplate.keys(REDIS_CHAT_MEMORY_PREFIX + "*");
        if (redisKeys == null || redisKeys.isEmpty()) {
            log.info("Redis中没有找到对话数据");
            return Map.of("total", 0, "success", 0, "failed", 0);
        }
        
        int total = redisKeys.size();
        int success = 0;
        int failed = 0;
        
        for (String redisKey : redisKeys) {
            String sessionId = redisKey.replace(REDIS_CHAT_MEMORY_PREFIX, "");
            
            try {
                if (syncConversation(sessionId)) {
                    success++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                log.error("同步会话时出错: sessionId={}, error={}", sessionId, e.getMessage());
                failed++;
            }
        }
        
        log.info("批量同步完成: total={}, success={}, failed={}", total, success, failed);
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
        
        log.info("【buildConversation】开始构建对话记录");
        log.info("参数: sessionId='{}', messageCount={}, userId='{}', characterId={}", 
            sessionId, messageStrings.size(), info.userId, info.characterId);
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minusMinutes(30); // 估算开始时间
        log.info("时间设置: startTime={}, endTime={}", startTime, now);
        
        // 生成对话标题
        log.info("生成对话标题...");
        String title = generateConversationTitle(messageStrings, info.characterId);
        log.info("生成标题: '{}'", title);
        
        // 计算统计信息
        log.info("计算统计信息...");
        int messageCount = messageStrings.size();
        int totalTokens = estimateTokenCount(messageStrings);
        log.info("统计信息: messageCount={}, totalTokens={}", messageCount, totalTokens);
        
        // 生成摘要
        log.info("生成对话摘要...");
        String summary = generateConversationSummary(messageStrings);
        log.info("生成摘要: '{}'", summary);
        
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
            
        log.info("【buildConversation】对话记录构建完成");
        return conversation;
    }
    
    /**
     * 构建对话消息列表
     */
    private List<ConversationMessage> buildConversationMessages(Long conversationId, 
                                                               String sessionId, 
                                                               List<String> messageStrings) {
        log.info("【buildConversationMessages】开始构建消息记录列表");
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
                log.error("【ERROR】解析消息失败: index={}, error={}", i, e.getMessage(), e);
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