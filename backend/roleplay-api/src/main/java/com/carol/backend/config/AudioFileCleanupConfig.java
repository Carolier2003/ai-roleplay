package com.carol.backend.config;

import com.carol.backend.service.IAudioFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 音频文件清理配置
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.audio.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class AudioFileCleanupConfig {
    
    private final IAudioFileService audioFileService;
    
    /**
     * 每天凌晨2点清理7天前的音频文件
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldAudioFiles() {
        log.info("[cleanupOldAudioFiles] 开始清理旧的音频文件");
        
        try {
            // 清理7天前的文件
            audioFileService.cleanupOldFiles(7);
            log.info("[cleanupOldAudioFiles] 音频文件清理完成");
        } catch (Exception e) {
            log.error("[cleanupOldAudioFiles] 音频文件清理失败", e);
        }
    }
    
    /**
     * 每小时清理30天前的音频文件（更保守的清理）
     */
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void cleanupVeryOldAudioFiles() {
        try {
            // 清理30天前的文件
            audioFileService.cleanupOldFiles(30);
        } catch (Exception e) {
            log.warn("[cleanupVeryOldAudioFiles] 清理超旧音频文件失败", e);
        }
    }
}
