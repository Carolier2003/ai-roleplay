package com.carol.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 临时调试配置 - 打印API密钥信息
 */
@Slf4j
@Component
public class ApiKeyDebugConfig implements ApplicationRunner {

    @Value("${spring.ai.dashscope.api-key:UNSET}")
    private String apiKey;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("=== API密钥调试信息 ===");
        
        if (apiKey == null || "UNSET".equals(apiKey)) {
            log.error("❌ API密钥未设置或为空: {}", apiKey);
        } else if (apiKey.length() < 16) {
            log.warn("⚠️ API密钥长度可能不正确: length={}, value={}", apiKey.length(), apiKey);
        } else {
            // 中间8位打星号
            String maskedKey = apiKey.substring(0, 8) + "********" + apiKey.substring(Math.max(16, apiKey.length() - 8));
            log.info("✅ API密钥已读取: {}", maskedKey);
        }
        
        // 同时检查环境变量
        String envKey = System.getenv("AI_DASHSCOPE_API_KEY");
        if (envKey != null) {
            String maskedEnvKey = envKey.substring(0, 8) + "********" + envKey.substring(Math.max(16, envKey.length() - 8));
            log.info("🔍 环境变量 AI_DASHSCOPE_API_KEY: {}", maskedEnvKey);
        } else {
            log.warn("⚠️ 环境变量 AI_DASHSCOPE_API_KEY 未设置");
        }
        
        log.info("========================");
    }
}
