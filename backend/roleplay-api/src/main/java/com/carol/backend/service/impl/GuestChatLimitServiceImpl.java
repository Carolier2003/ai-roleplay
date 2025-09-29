package com.carol.backend.service.impl;

import com.carol.backend.service.IGuestChatLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 游客聊天限制服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuestChatLimitServiceImpl implements IGuestChatLimitService {
    
    private final StringRedisTemplate stringRedisTemplate;
    
    private static final String GUEST_CHAT_COUNT_PREFIX = "guest_chat_count:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @Override
    public boolean canGuestChat(String sessionId) {
        int currentCount = getGuestChatCount(sessionId);
        boolean canChat = currentCount < MAX_GUEST_CHAT_COUNT;
        
        log.info("[canGuestChat] 游客聊天检查: sessionId={}, currentCount={}, canChat={}", 
                sessionId, currentCount, canChat);
        
        return canChat;
    }
    
    @Override
    public void incrementGuestChatCount(String sessionId) {
        String key = buildRedisKey(sessionId);
        
        try {
            Long newCount = stringRedisTemplate.opsForValue().increment(key);
            
            // 设置过期时间为当天结束
            if (newCount == 1) {
                // 第一次设置，设置过期时间到当天结束
                long secondsUntilEndOfDay = getSecondsUntilEndOfDay();
                stringRedisTemplate.expire(key, secondsUntilEndOfDay, TimeUnit.SECONDS);
            }
            
            log.info("[incrementGuestChatCount] 游客聊天次数增加: sessionId={}, newCount={}", 
                    sessionId, newCount);
            
        } catch (Exception e) {
            log.error("[incrementGuestChatCount] 增加游客聊天次数失败: sessionId={}, error={}", 
                    sessionId, e.getMessage(), e);
        }
    }
    
    @Override
    public int getGuestChatCount(String sessionId) {
        String key = buildRedisKey(sessionId);
        
        try {
            String countStr = stringRedisTemplate.opsForValue().get(key);
            int count = countStr != null ? Integer.parseInt(countStr) : 0;
            
            log.debug("[getGuestChatCount] 获取游客聊天次数: sessionId={}, count={}", 
                    sessionId, count);
            
            return count;
            
        } catch (Exception e) {
            log.error("[getGuestChatCount] 获取游客聊天次数失败: sessionId={}, error={}", 
                    sessionId, e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * 构建Redis键
     */
    private String buildRedisKey(String sessionId) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        return GUEST_CHAT_COUNT_PREFIX + today + ":" + sessionId;
    }
    
    /**
     * 获取到当天结束的秒数
     */
    private long getSecondsUntilEndOfDay() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        long tomorrowEpochSecond = tomorrow.atStartOfDay().toEpochSecond(
                java.time.ZoneOffset.systemDefault().getRules().getOffset(java.time.Instant.now()));
        long nowEpochSecond = java.time.Instant.now().getEpochSecond();
        return tomorrowEpochSecond - nowEpochSecond;
    }
}
