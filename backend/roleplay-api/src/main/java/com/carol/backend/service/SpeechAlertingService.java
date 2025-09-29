package com.carol.backend.service;

import com.carol.backend.config.SpeechPerformanceConfig;
import com.carol.backend.service.SpeechMetricsCollector.PerformanceSnapshot;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语音识别告警服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechAlertingService {
    
    private final SpeechPerformanceConfig performanceConfig;
    private final SpeechMetricsCollector metricsCollector;
    
    // 告警状态管理
    private final Map<AlertType, AlertState> alertStates = new ConcurrentHashMap<>();
    
    // 告警历史
    private final List<AlertRecord> alertHistory = new ArrayList<>();
    private final Object alertHistoryLock = new Object();
    
    // 告警监听器
    private final List<AlertListener> alertListeners = new ArrayList<>();
    
    /**
     * 定期检查告警条件
     */
    @Scheduled(fixedRateString = "#{${app.speech.performance.alerting.alert-check-interval-minutes:5} * 60 * 1000}")
    public void checkAlerts() {
        if (!performanceConfig.getAlerting().isEnabled()) {
            return;
        }
        
        log.debug("开始检查告警条件");
        
        PerformanceSnapshot snapshot = metricsCollector.getCurrentMetrics();
        
        // 检查各种告警条件
        checkFailureRateAlert(snapshot);
        checkLatencyAlert(snapshot);
        checkMemoryUsageAlert();
        checkConcurrentConnectionsAlert(snapshot);
        
        log.debug("告警检查完成");
    }
    
    /**
     * 检查失败率告警
     */
    private void checkFailureRateAlert(PerformanceSnapshot snapshot) {
        double threshold = performanceConfig.getAlerting().getFailureRateThreshold();
        double currentRate = snapshot.getRecentSuccessRate() > 0 ? 
            (100 - snapshot.getRecentSuccessRate()) : snapshot.getFailureRate();
        
        if (currentRate >= threshold) {
            triggerAlert(AlertType.HIGH_FAILURE_RATE, 
                AlertSeverity.WARNING,
                String.format("失败率过高: %.2f%% (阈值: %.2f%%)", currentRate, threshold),
                createFailureRateDetails(snapshot, currentRate)
            );
        } else {
            resolveAlert(AlertType.HIGH_FAILURE_RATE);
        }
    }
    
    /**
     * 检查延迟告警
     */
    private void checkLatencyAlert(PerformanceSnapshot snapshot) {
        long threshold = performanceConfig.getAlerting().getAvgLatencyThresholdMs();
        double currentLatency = snapshot.getRecentAverageLatency() > 0 ? 
            snapshot.getRecentAverageLatency() : snapshot.getAverageLatency();
        
        if (currentLatency >= threshold) {
            triggerAlert(AlertType.HIGH_LATENCY,
                AlertSeverity.WARNING,
                String.format("平均延迟过高: %.0fms (阈值: %dms)", currentLatency, threshold),
                createLatencyDetails(snapshot, currentLatency)
            );
        } else {
            resolveAlert(AlertType.HIGH_LATENCY);
        }
    }
    
    /**
     * 检查内存使用率告警
     */
    private void checkMemoryUsageAlert() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double usagePercentage = (double) usedMemory / totalMemory * 100;
        
        double threshold = performanceConfig.getAlerting().getMemoryUsageThreshold();
        
        if (usagePercentage >= threshold) {
            triggerAlert(AlertType.HIGH_MEMORY_USAGE,
                AlertSeverity.CRITICAL,
                String.format("内存使用率过高: %.2f%% (阈值: %.2f%%)", usagePercentage, threshold),
                createMemoryDetails(usedMemory, totalMemory, usagePercentage)
            );
        } else {
            resolveAlert(AlertType.HIGH_MEMORY_USAGE);
        }
    }
    
    /**
     * 检查并发连接数告警
     */
    private void checkConcurrentConnectionsAlert(PerformanceSnapshot snapshot) {
        int threshold = performanceConfig.getAlerting().getConcurrentConnectionsThreshold();
        long currentConnections = snapshot.getCurrentConcurrentRequests();
        
        if (currentConnections >= threshold) {
            triggerAlert(AlertType.HIGH_CONCURRENT_CONNECTIONS,
                AlertSeverity.WARNING,
                String.format("并发连接数过高: %d (阈值: %d)", currentConnections, threshold),
                createConcurrentConnectionsDetails(snapshot, currentConnections)
            );
        } else {
            resolveAlert(AlertType.HIGH_CONCURRENT_CONNECTIONS);
        }
    }
    
    /**
     * 触发告警
     */
    private void triggerAlert(AlertType alertType, AlertSeverity severity, String message, Map<String, Object> details) {
        AlertState currentState = alertStates.get(alertType);
        LocalDateTime now = LocalDateTime.now();
        
        // 检查冷却时间
        if (currentState != null && currentState.isActive()) {
            int cooldownMinutes = performanceConfig.getAlerting().getAlertCooldownMinutes();
            if (currentState.getLastTriggeredTime().plusMinutes(cooldownMinutes).isAfter(now)) {
                log.debug("告警 {} 仍在冷却期内", alertType);
                return;
            }
        }
        
        // 创建新的告警状态
        AlertState newState = new AlertState();
        newState.setActive(true);
        newState.setFirstTriggeredTime(currentState != null ? 
            currentState.getFirstTriggeredTime() : now);
        newState.setLastTriggeredTime(now);
        newState.setTriggerCount(currentState != null ? 
            currentState.getTriggerCount() + 1 : 1);
        
        alertStates.put(alertType, newState);
        
        // 创建告警记录
        AlertRecord record = new AlertRecord();
        record.setAlertType(alertType);
        record.setSeverity(severity);
        record.setMessage(message);
        record.setDetails(details);
        record.setTimestamp(now);
        record.setActive(true);
        
        // 添加到历史记录
        synchronized (alertHistoryLock) {
            alertHistory.add(record);
            
            // 保持最近1000条记录
            if (alertHistory.size() > 1000) {
                alertHistory.remove(0);
            }
        }
        
        log.warn("触发告警: type={}, severity={}, message={}", alertType, severity, message);
        
        // 通知监听器
        notifyAlertListeners(record);
    }
    
    /**
     * 解决告警
     */
    private void resolveAlert(AlertType alertType) {
        AlertState currentState = alertStates.get(alertType);
        
        if (currentState != null && currentState.isActive()) {
            currentState.setActive(false);
            currentState.setResolvedTime(LocalDateTime.now());
            
            log.info("告警已解决: type={}", alertType);
            
            // 创建解决记录
            AlertRecord record = new AlertRecord();
            record.setAlertType(alertType);
            record.setSeverity(AlertSeverity.INFO);
            record.setMessage("告警已解决");
            record.setTimestamp(LocalDateTime.now());
            record.setActive(false);
            
            synchronized (alertHistoryLock) {
                alertHistory.add(record);
            }
            
            // 通知监听器
            notifyAlertListeners(record);
        }
    }
    
    /**
     * 通知告警监听器
     */
    private void notifyAlertListeners(AlertRecord record) {
        for (AlertListener listener : alertListeners) {
            try {
                listener.onAlert(record);
            } catch (Exception e) {
                log.error("通知告警监听器时发生错误", e);
            }
        }
    }
    
    /**
     * 添加告警监听器
     */
    public void addAlertListener(AlertListener listener) {
        alertListeners.add(listener);
    }
    
    /**
     * 移除告警监听器
     */
    public void removeAlertListener(AlertListener listener) {
        alertListeners.remove(listener);
    }
    
    /**
     * 获取当前活跃告警
     */
    public List<AlertRecord> getActiveAlerts() {
        synchronized (alertHistoryLock) {
            return alertHistory.stream()
                    .filter(AlertRecord::isActive)
                    .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                    .toList();
        }
    }
    
    /**
     * 获取告警历史
     */
    public List<AlertRecord> getAlertHistory(int hours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        
        synchronized (alertHistoryLock) {
            return alertHistory.stream()
                    .filter(record -> record.getTimestamp().isAfter(cutoff))
                    .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                    .toList();
        }
    }
    
    /**
     * 获取告警统计
     */
    public AlertStatistics getAlertStatistics(int hours) {
        List<AlertRecord> records = getAlertHistory(hours);
        
        AlertStatistics stats = new AlertStatistics();
        stats.setTotalAlerts(records.size());
        stats.setActiveAlerts(records.stream().mapToInt(r -> r.isActive() ? 1 : 0).sum());
        
        Map<AlertType, Long> alertsByType = new HashMap<>();
        Map<AlertSeverity, Long> alertsBySeverity = new HashMap<>();
        
        for (AlertRecord record : records) {
            alertsByType.merge(record.getAlertType(), 1L, Long::sum);
            alertsBySeverity.merge(record.getSeverity(), 1L, Long::sum);
        }
        
        stats.setAlertsByType(alertsByType);
        stats.setAlertsBySeverity(alertsBySeverity);
        
        return stats;
    }
    
    // 创建详情方法
    private Map<String, Object> createFailureRateDetails(PerformanceSnapshot snapshot, double currentRate) {
        Map<String, Object> details = new HashMap<>();
        details.put("currentFailureRate", currentRate);
        details.put("threshold", performanceConfig.getAlerting().getFailureRateThreshold());
        details.put("totalRequests", snapshot.getTotalRequests());
        details.put("failedRequests", snapshot.getFailedRequests());
        details.put("errorCounts", snapshot.getErrorCounts());
        return details;
    }
    
    private Map<String, Object> createLatencyDetails(PerformanceSnapshot snapshot, double currentLatency) {
        Map<String, Object> details = new HashMap<>();
        details.put("currentLatency", currentLatency);
        details.put("threshold", performanceConfig.getAlerting().getAvgLatencyThresholdMs());
        details.put("maxLatency", snapshot.getMaxLatency());
        details.put("minLatency", snapshot.getMinLatency());
        return details;
    }
    
    private Map<String, Object> createMemoryDetails(long usedMemory, long totalMemory, double usagePercentage) {
        Map<String, Object> details = new HashMap<>();
        details.put("usedMemoryMB", usedMemory / (1024 * 1024));
        details.put("totalMemoryMB", totalMemory / (1024 * 1024));
        details.put("usagePercentage", usagePercentage);
        details.put("threshold", performanceConfig.getAlerting().getMemoryUsageThreshold());
        return details;
    }
    
    private Map<String, Object> createConcurrentConnectionsDetails(PerformanceSnapshot snapshot, long currentConnections) {
        Map<String, Object> details = new HashMap<>();
        details.put("currentConnections", currentConnections);
        details.put("threshold", performanceConfig.getAlerting().getConcurrentConnectionsThreshold());
        details.put("maxConnections", snapshot.getMaxConcurrentRequests());
        return details;
    }
    
    /**
     * 告警类型
     */
    public enum AlertType {
        HIGH_FAILURE_RATE,
        HIGH_LATENCY,
        HIGH_MEMORY_USAGE,
        HIGH_CONCURRENT_CONNECTIONS
    }
    
    /**
     * 告警严重级别
     */
    public enum AlertSeverity {
        INFO,
        WARNING,
        CRITICAL
    }
    
    /**
     * 告警状态
     */
    @Data
    public static class AlertState {
        private boolean active;
        private LocalDateTime firstTriggeredTime;
        private LocalDateTime lastTriggeredTime;
        private LocalDateTime resolvedTime;
        private int triggerCount;
    }
    
    /**
     * 告警记录
     */
    @Data
    public static class AlertRecord {
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
    public static class AlertStatistics {
        private int totalAlerts;
        private int activeAlerts;
        private Map<AlertType, Long> alertsByType;
        private Map<AlertSeverity, Long> alertsBySeverity;
    }
    
    /**
     * 告警监听器接口
     */
    public interface AlertListener {
        void onAlert(AlertRecord record);
    }
}
