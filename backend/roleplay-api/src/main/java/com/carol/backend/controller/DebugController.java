package com.carol.backend.controller;

import com.carol.backend.mapper.ConversationMapper;
import com.carol.backend.service.IConversationSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 调试控制器
 * 用于测试各个组件的连接状态
 * 
 * @author carol
 */
@Slf4j
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {
    
    private final ConversationMapper conversationMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final IConversationSyncService syncService;
    
    /**
     * 测试数据库连接
     */
    @GetMapping("/database")
    public Map<String, Object> testDatabase() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 测试数据库连接
            int count = conversationMapper.countBySessionId("test");
            result.put("database_status", "connected");
            result.put("test_query_result", count);
            result.put("timestamp", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("数据库连接测试失败", e);
            result.put("database_status", "error");
            result.put("error_message", e.getMessage());
            result.put("error_class", e.getClass().getSimpleName());
            result.put("timestamp", LocalDateTime.now());
        }
        
        return result;
    }
    
    /**
     * 测试Redis连接
     */
    @GetMapping("/redis")
    public Map<String, Object> testRedis() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 测试Redis连接
            Set<String> keys = redisTemplate.keys("spring_ai_alibaba_chat_memory:*");
            result.put("redis_status", "connected");
            result.put("chat_memory_keys_count", keys != null ? keys.size() : 0);
            result.put("sample_keys", keys != null ? keys.stream().limit(3).toArray() : new String[0]);
            result.put("timestamp", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("Redis连接测试失败", e);
            result.put("redis_status", "error");
            result.put("error_message", e.getMessage());
            result.put("error_class", e.getClass().getSimpleName());
            result.put("timestamp", LocalDateTime.now());
        }
        
        return result;
    }
    
    /**
     * 综合状态检查
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> result = new HashMap<>();
        
        // 测试数据库
        try {
            conversationMapper.countBySessionId("test");
            result.put("database", "OK");
        } catch (Exception e) {
            result.put("database", "ERROR: " + e.getMessage());
        }
        
        // 测试Redis
        try {
            Set<String> keys = redisTemplate.keys("test:*");
            result.put("redis", "OK");
        } catch (Exception e) {
            result.put("redis", "ERROR: " + e.getMessage());
        }
        
        result.put("timestamp", LocalDateTime.now());
        result.put("overall_status", result.values().stream().allMatch(v -> v.toString().startsWith("OK")) ? "HEALTHY" : "UNHEALTHY");
        
        return result;
    }
    
    /**
     * 测试同步功能的详细调试
     */
    @GetMapping("/sync/test/{sessionId}")
    public Map<String, Object> testSyncFunction(@PathVariable String sessionId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 测试Redis读取
            String redisKey = "spring_ai_alibaba_chat_memory:" + sessionId;
            log.info("测试Redis读取，key: {}", redisKey);
            
            // 使用StringRedisTemplate读取
            java.util.List<String> messageStrings = stringRedisTemplate.opsForList().range(redisKey, 0, -1);
            
            result.put("redis_key", redisKey);
            result.put("messages_found", messageStrings != null ? messageStrings.size() : 0);
            result.put("first_message_sample", messageStrings != null && !messageStrings.isEmpty() ? 
                messageStrings.get(0).substring(0, Math.min(100, messageStrings.get(0).length())) + "..." : "无消息");
            
            // 2. 测试会话解析
            if (sessionId.matches("user_([^_]+)_(?:char_(\\d+)|general)")) {
                result.put("session_id_format", "VALID");
            } else {
                result.put("session_id_format", "INVALID");
            }
            
            // 3. 测试数据库检查
            int existingCount = conversationMapper.countBySessionId(sessionId);
            result.put("existing_in_db", existingCount);
            
            // 4. 尝试同步
            try {
                boolean syncResult = syncService.syncConversation(sessionId);
                result.put("sync_result", syncResult);
                result.put("sync_status", syncResult ? "SUCCESS" : "FAILED");
            } catch (Exception e) {
                result.put("sync_result", false);
                result.put("sync_status", "EXCEPTION");
                result.put("sync_error", e.getMessage());
                result.put("sync_error_class", e.getClass().getSimpleName());
                log.error("同步测试异常", e);
            }
            
        } catch (Exception e) {
            log.error("调试测试失败", e);
            result.put("debug_status", "ERROR");
            result.put("error_message", e.getMessage());
            result.put("error_class", e.getClass().getSimpleName());
        }
        
        result.put("timestamp", LocalDateTime.now());
        return result;
    }
}
