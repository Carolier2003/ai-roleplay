package com.carol.backend.config;

import com.alibaba.cloud.ai.memory.redis.RedissonRedisChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 会话记忆配置类
 * 配置Redis作为会话记忆存储
 * 
 * @author carol
 */
@Configuration
public class ChatMemoryConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;
    
    @Value("${spring.data.redis.port:6379}")
    private int redisPort;
    
    @Value("${spring.data.redis.password:}")
    private String redisPassword;
    
    @Value("${spring.data.redis.timeout:5000ms}")
    private String redisTimeout;
    
    @Value("${app.chat.memory.max-messages:100}")
    private int maxMessages;

    /**
     * 配置Redis会话记忆存储库
     */
    @Bean
    public RedissonRedisChatMemoryRepository redisChatMemoryRepository() {
        try {
            // 尝试使用静态方法创建
            if (redisPassword != null && !redisPassword.trim().isEmpty()) {
                return RedissonRedisChatMemoryRepository.builder()
                        .host(redisHost)
                        .port(redisPort)
                        .password(redisPassword)
                        .timeout(parseTimeout(redisTimeout))
                        .build();
            } else {
                return RedissonRedisChatMemoryRepository.builder()
                        .host(redisHost)
                        .port(redisPort)
                        .timeout(parseTimeout(redisTimeout))
                        .build();
            }
        } catch (Exception e) {
            // 如果builder方法不存在，尝试使用构造函数
            throw new RuntimeException("无法创建RedissonRedisChatMemoryRepository: " + e.getMessage(), e);
        }
    }
    
    /**
     * 配置MessageWindowChatMemory Bean
     */
    @Bean
    public MessageWindowChatMemory messageWindowChatMemory(RedissonRedisChatMemoryRepository redisChatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(redisChatMemoryRepository)
                .maxMessages(maxMessages > 0 ? maxMessages : 100)
                .build();
    }
    
    /**
     * 解析超时时间配置（支持ms后缀）
     */
    private int parseTimeout(String timeoutStr) {
        if (timeoutStr.endsWith("ms")) {
            return Integer.parseInt(timeoutStr.substring(0, timeoutStr.length() - 2));
        }
        return Integer.parseInt(timeoutStr);
    }
}
