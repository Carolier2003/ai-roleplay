package com.carol.backend.service;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 语音识别告警服务接口
 * 
 * @author jianjl
 * @version 1.0
 * @description 语音识别告警服务
 * @date 2025-01-15
 */
public interface ISpeechAlertingService {
    
    /**
     * 定期检查告警条件
     */
    void checkAlerts();
    
    /**
     * 添加告警监听器
     * 
     * @param listener 监听器
     */
    void addAlertListener(AlertListener listener);
    
    /**
     * 移除告警监听器
     * 
     * @param listener 监听器
     */
    void removeAlertListener(AlertListener listener);
    
    /**
     * 获取当前活跃告警
     * 
     * @return 活跃告警列表
     */
    List<AlertRecord> getActiveAlerts();
    
    /**
     * 获取告警历史
     * 
     * @param hours 小时数
     * @return 告警历史列表
     */
    List<AlertRecord> getAlertHistory(int hours);
    
    /**
     * 获取告警统计
     * 
     * @param hours 小时数
     * @return 告警统计
     */
    AlertStatistics getAlertStatistics(int hours);
    
    /**
     * 告警类型
     */
    enum AlertType {
        HIGH_FAILURE_RATE,
        HIGH_LATENCY,
        HIGH_MEMORY_USAGE,
        HIGH_CONCURRENT_CONNECTIONS
    }
    
    /**
     * 告警严重级别
     */
    enum AlertSeverity {
        INFO,
        WARNING,
        CRITICAL
    }
    
    /**
     * 告警监听器接口
     */
    interface AlertListener {
        void onAlert(AlertRecord record);
    }
    
    /**
     * 告警记录
     */
    @Data
    class AlertRecord {
        private AlertType alertType;
        private AlertSeverity severity;
        private String message;
        private Map<String, Object> details;
        private LocalDateTime timestamp;
        private boolean active;
    }
    
    /**
     * 告警统计
     */
    @Data
    class AlertStatistics {
        private int totalAlerts;
        private int activeAlerts;
        private Map<AlertType, Long> alertsByType;
        private Map<AlertSeverity, Long> alertsBySeverity;
    }
}
