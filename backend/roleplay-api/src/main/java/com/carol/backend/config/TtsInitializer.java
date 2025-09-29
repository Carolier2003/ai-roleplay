package com.carol.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * TTS语音合成初始化器
 * 负责初始化角色音色映射等TTS相关配置
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TtsInitializer implements ApplicationRunner {
    
    private final TtsSynthesisConfig ttsSynthesisConfig;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("=== 初始化TTS语音合成配置 ===");
        
        try {
            // 初始化默认角色音色映射
            ttsSynthesisConfig.initDefaultCharacterVoices();
            log.info("✅ 角色音色映射初始化完成");
            
            // 打印角色音色映射信息
            logCharacterVoiceMappings();
            
            log.info("=== TTS语音合成配置初始化完成 ===");
            
        } catch (Exception e) {
            log.error("TTS语音合成配置初始化失败", e);
            throw e;
        }
    }
    
    /**
     * 打印角色音色映射信息
     */
    private void logCharacterVoiceMappings() {
        log.info("📋 角色音色映射配置:");
        log.info("  角色ID 1 (哈利·波特) -> 音色: {}", ttsSynthesisConfig.getCharacterVoice(1L));
        log.info("  角色ID 2 (苏格拉底) -> 音色: {}", ttsSynthesisConfig.getCharacterVoice(2L));
        log.info("  角色ID 3 (爱因斯坦) -> 音色: {}", ttsSynthesisConfig.getCharacterVoice(3L));
        log.info("  角色ID 4 (江户川柯南) -> 音色: {}", ttsSynthesisConfig.getCharacterVoice(4L));
        log.info("  角色ID 5 (泰拉瑞亚向导) -> 音色: {}", ttsSynthesisConfig.getCharacterVoice(5L));
        log.info("  默认音色: {}", ttsSynthesisConfig.getDefaultVoice());
    }
}
