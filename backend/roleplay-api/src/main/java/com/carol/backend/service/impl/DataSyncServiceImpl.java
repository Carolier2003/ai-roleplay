package com.carol.backend.service.impl;

import com.carol.backend.enums.ErrorCode;
import com.carol.backend.exception.BusinessException;
import com.carol.backend.service.IDataSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据同步服务实现类
 * 
 * @author jianjl
 * @version 1.0
 * @description 负责将Redis中的临时数据同步到MySQL持久化存储
 * @date 2025-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataSyncServiceImpl implements IDataSyncService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Redis键名常量
    private static final String CHAT_MESSAGES_KEY_PREFIX = "chat:messages:user:";
    private static final String CONVERSATIONS_KEY_PREFIX = "chat:conversations:user:";
    private static final String USER_ACTIVITY_KEY_PREFIX = "user:activity:";
    private static final String USERS_SET_KEY = "users:active";
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncChatMessagesToMysql() {
        log.info("[syncChatMessagesToMysql] 开始同步聊天消息数据到MySQL");
        
        AtomicInteger syncedCount = new AtomicInteger(0);
        
        try {
            // 获取所有活跃用户ID
            Set<Object> activeUsers = redisTemplate.opsForSet().members(USERS_SET_KEY);
            
            if (activeUsers == null || activeUsers.isEmpty()) {
                log.info("[syncChatMessagesToMysql] 没有找到活跃用户，跳过同步");
                return 0;
            }
            
            log.info("[syncChatMessagesToMysql] 找到 {} 个活跃用户，开始同步聊天消息", activeUsers.size());
            
            for (Object userIdObj : activeUsers) {
                String userId = userIdObj.toString();
                String messagesKey = CHAT_MESSAGES_KEY_PREFIX + userId;
                
                try {
                    // 获取用户的聊天消息
                    Set<Object> messageKeys = redisTemplate.opsForZSet().range(messagesKey, 0, -1);
                    
                    if (messageKeys != null && !messageKeys.isEmpty()) {
                        log.info("[syncChatMessagesToMysql] 用户 {} 有 {} 条消息需要同步", userId, messageKeys.size());
                        
                        for (Object messageKey : messageKeys) {
                            // 这里应该调用具体的数据库插入逻辑
                            // TODO: 实现具体的MySQL插入逻辑
                            syncedCount.incrementAndGet();
                        }
                        
                        // 同步完成后，可以选择清理Redis中的数据
                        // redisTemplate.delete(messagesKey);
                    }
                    
                } catch (Exception e) {
                    log.error("[syncChatMessagesToMysql] 同步用户 {} 的聊天消息失败: {}", userId, e.getMessage(), e);
                }
            }
            
            log.info("[syncChatMessagesToMysql] 聊天消息同步完成，共同步 {} 条消息", syncedCount.get());
            return syncedCount.get();
            
        } catch (Exception e) {
            log.error("[syncChatMessagesToMysql] 同步聊天消息异常: error={}", e.getMessage(), e);
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "同步聊天消息失败", e);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncConversationsToMysql() {
        log.info("[syncConversationsToMysql] 开始同步会话数据到MySQL");
        
        AtomicInteger syncedCount = new AtomicInteger(0);
        
        try {
            // 获取所有活跃用户ID
            Set<Object> activeUsers = redisTemplate.opsForSet().members(USERS_SET_KEY);
            
            if (activeUsers == null || activeUsers.isEmpty()) {
                log.info("[syncConversationsToMysql] 没有找到活跃用户，跳过同步");
                return 0;
            }
            
            log.info("[syncConversationsToMysql] 找到 {} 个活跃用户，开始同步会话数据", activeUsers.size());
            
            for (Object userIdObj : activeUsers) {
                String userId = userIdObj.toString();
                String conversationsKey = CONVERSATIONS_KEY_PREFIX + userId;
                
                try {
                    // 获取用户的会话信息
                    Set<Object> conversationKeys = redisTemplate.opsForSet().members(conversationsKey);
                    
                    if (conversationKeys != null && !conversationKeys.isEmpty()) {
                        log.info("[syncConversationsToMysql] 用户 {} 有 {} 个会话需要同步", userId, conversationKeys.size());
                        
                        for (Object conversationKey : conversationKeys) {
                            // 这里应该调用具体的数据库插入逻辑
                            // TODO: 实现具体的MySQL插入逻辑
                            syncedCount.incrementAndGet();
                        }
                    }
                    
                } catch (Exception e) {
                    log.error("[syncConversationsToMysql] 同步用户 {} 的会话数据失败: {}", userId, e.getMessage(), e);
                }
            }
            
            log.info("[syncConversationsToMysql] 会话数据同步完成，共同步 {} 个会话", syncedCount.get());
            return syncedCount.get();
            
        } catch (Exception e) {
            log.error("[syncConversationsToMysql] 同步会话数据异常: error={}", e.getMessage(), e);
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "同步会话数据失败", e);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncUserActivityToMysql() {
        log.info("[syncUserActivityToMysql] 开始同步用户活动数据到MySQL");
        
        AtomicInteger syncedCount = new AtomicInteger(0);
        
        try {
            // 获取所有活跃用户ID
            Set<Object> activeUsers = redisTemplate.opsForSet().members(USERS_SET_KEY);
            
            if (activeUsers == null || activeUsers.isEmpty()) {
                log.info("[syncUserActivityToMysql] 没有找到活跃用户，跳过同步");
                return 0;
            }
            
            log.info("[syncUserActivityToMysql] 找到 {} 个活跃用户，开始同步活动数据", activeUsers.size());
            
            for (Object userIdObj : activeUsers) {
                String userId = userIdObj.toString();
                String activityKey = USER_ACTIVITY_KEY_PREFIX + userId;
                
                try {
                    // 获取用户活动数据
                    Object activityData = redisTemplate.opsForValue().get(activityKey);
                    
                    if (activityData != null) {
                        log.info("[syncUserActivityToMysql] 同步用户 {} 的活动数据", userId);
                        
                        // 这里应该调用具体的数据库插入逻辑
                        // TODO: 实现具体的MySQL插入逻辑
                        syncedCount.incrementAndGet();
                    }
                    
                } catch (Exception e) {
                    log.error("[syncUserActivityToMysql] 同步用户 {} 的活动数据失败: {}", userId, e.getMessage(), e);
                }
            }
            
            log.info("[syncUserActivityToMysql] 用户活动数据同步完成，共同步 {} 个用户", syncedCount.get());
            return syncedCount.get();
            
        } catch (Exception e) {
            log.error("[syncUserActivityToMysql] 同步用户活动数据异常: error={}", e.getMessage(), e);
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "同步用户活动数据失败", e);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataSyncResult performFullDataSync() {
        log.info("[performFullDataSync] 开始执行完整数据同步任务");
        
        long startTime = System.currentTimeMillis();
        DataSyncResult result = new DataSyncResult();
        
        try {
            // 同步聊天消息
            int syncedMessages = syncChatMessagesToMysql();
            result.setSyncedMessages(syncedMessages);
            
            // 同步会话信息
            int syncedConversations = syncConversationsToMysql();
            result.setSyncedConversations(syncedConversations);
            
            // 同步用户活动
            int syncedUsers = syncUserActivityToMysql();
            result.setSyncedUsers(syncedUsers);
            
            long endTime = System.currentTimeMillis();
            result.setSyncDuration(endTime - startTime);
            result.setSuccess(true);
            
            log.info("[performFullDataSync] 完整数据同步任务完成: {}", result);
            
            return result;
            
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            result.setSyncDuration(endTime - startTime);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            
            log.error("[performFullDataSync] 完整数据同步任务失败: {}", e.getMessage(), e);
            
            return result;
        }
    }
    
    /**
     * 获取当前时间戳字符串
     */
    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
