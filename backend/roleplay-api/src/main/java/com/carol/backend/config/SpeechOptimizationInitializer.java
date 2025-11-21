package com.carol.backend.config;

import com.carol.backend.service.ISpeechAlertingService;
import com.carol.backend.service.ISpeechResourceManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 语音识别优化组件初始化器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpeechOptimizationInitializer implements ApplicationRunner {
    
    private final ISpeechResourceManager resourceManager;
    private final ISpeechAlertingService alertingService;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("=== 初始化语音识别优化组件 ===");
        
        try {
            // 初始化资源管理器
            resourceManager.init();
            log.info("✅ 资源管理器初始化完成");
            
            // 注册默认告警监听器
            registerDefaultAlertListeners();
            log.info("✅ 告警监听器注册完成");
            
            log.info("=== 语音识别优化组件初始化完成 ===");
            
        } catch (Exception e) {
            log.error("语音识别优化组件初始化失败", e);
            throw e;
        }
    }
    
    /**
     * 注册默认告警监听器
     */
    private void registerDefaultAlertListeners() {
        // 控制台告警监听器
        alertingService.addAlertListener(record -> {
            switch (record.getSeverity()) {
                case CRITICAL:
                    log.error("[registerDefaultAlertListeners] 🚨 [告警-严重] {}: {}", record.getAlertType(), record.getMessage());
                    break;
                case WARNING:
                    log.warn("[registerDefaultAlertListeners] ⚠️ [告警-警告] {}: {}", record.getAlertType(), record.getMessage());
                    break;
                case INFO:
                    log.info("[registerDefaultAlertListeners] ℹ️ [告警-信息] {}: {}", record.getAlertType(), record.getMessage());
                    break;
            }
            
            // 如果有详细信息，也记录下来
            if (record.getDetails() != null && !record.getDetails().isEmpty()) {
                log.info("[registerDefaultAlertListeners] 📋 [告警详情] {}", record.getDetails());
            }
        });
        
        // 可以在这里添加其他告警监听器，比如：
        // - 发送邮件通知
        // - 推送到监控系统
        // - 写入告警数据库
        // - 调用外部API
    }
    
    /**
     * 应用关闭时清理资源
     */
    @EventListener
    public void handleContextClosed(ContextClosedEvent event) {
        log.info("=== 清理语音识别优化组件资源 ===");
        
        try {
            resourceManager.shutdown();
            log.info("✅ 资源管理器已关闭");
            
            log.info("=== 语音识别优化组件资源清理完成 ===");
            
        } catch (Exception e) {
            log.error("清理语音识别优化组件资源时发生错误", e);
        }
    }
}
