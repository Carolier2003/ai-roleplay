package com.carol.backend.service.impl;

import com.carol.backend.config.SpeechPerformanceConfig;
import com.carol.backend.service.ISpeechAlertingService;
import com.carol.backend.service.ISpeechMetricsCollector;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语音识别告警服务实现类
 * 
 * @author jianjl
 * @version 1.0
 * @description 语音识别告警服务实现
 * @date 2025-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechAlertingServiceImpl implements ISpeechAlertingService {
    
    private final SpeechPerformanceConfig performanceConfig;
    private final ISpeechMetricsCollector metricsCollector;
    
    // 告警状态管理
    private final Map<ISpeechAlertingService.AlertType, AlertState> alertStates = new ConcurrentHashMap<>();
    
    // 告警历史
    private final List<ISpeechAlertingService.AlertRecord> alertHistory = new ArrayList<>();
    private final Object alertHistoryLock = new Object();
    
    // 告警监听器
    private final List<ISpeechAlertingService.AlertListener> alertListeners = new ArrayList<>();
    
    @Override
    @Scheduled(fixedRateString = "#{${app.speech.performance.alerting.alert-check-interval-minutes:5} * 60 * 1000}")
    public void checkAlerts() {
        if (!performanceConfig.getAlerting().isEnabled()) {
            return;
        }
        
        log.debug("[checkAlerts] 开始检查告警条件");
        
        ISpeechMetricsCollector.PerformanceSnapshot snapshot = metricsCollector.getCurrentMetrics();
        
        // 检查各种告警条件
        checkFailureRateAlert(snapshot);
        checkLatencyAlert(snapshot);
        checkMemoryUsageAlert();
        checkConcurrentConnectionsAlert(snapshot);
        
        log.debug("[checkAlerts] 告警检查完成");
    }
    
    /**
     * 检查失败率告警
     */
    private void checkFailureRateAlert(ISpeechMetricsCollector.PerformanceSnapshot snapshot) {
        double threshold = performanceConfig.getAlerting().getFailureRateThreshold();
        double currentRate = snapshot.getRecentSuccessRate() > 0 ? 
            (100 - snapshot.getRecentSuccessRate()) : snapshot.getFailureRate();
        
        if (currentRate >= threshold) {
            triggerAlert(ISpeechAlertingService.AlertType.HIGH_FAILURE_RATE, 
                ISpeechAlertingService.AlertSeverity.WARNING,
                String.format("失败率过高: %.2f%% (阈值: %.2f%%)", currentRate, threshold),
                createFailureRateDetails(snapshot, currentRate)
            );
        } else {
            resolveAlert(ISpeechAlertingService.AlertType.HIGH_FAILURE_RATE);
        }
    }
    
    /**
     * 检查延迟告警
     */
    private void checkLatencyAlert(ISpeechMetricsCollector.PerformanceSnapshot snapshot) {
        long threshold = performanceConfig.getAlerting().getAvgLatencyThresholdMs();
        double currentLatency = snapshot.getRecentAverageLatency() > 0 ? 
            snapshot.getRecentAverageLatency() : snapshot.getAverageLatency();
        
        if (currentLatency >= threshold) {
            triggerAlert(ISpeechAlertingService.AlertType.HIGH_LATENCY,
                ISpeechAlertingService.AlertSeverity.WARNING,
                String.format("平均延迟过高: %.0fms (阈值: %dms)", currentLatency, threshold),
                createLatencyDetails(snapshot, currentLatency)
            );
        } else {
            resolveAlert(ISpeechAlertingService.AlertType.HIGH_LATENCY);
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
            triggerAlert(ISpeechAlertingService.AlertType.HIGH_MEMORY_USAGE,
                ISpeechAlertingService.AlertSeverity.CRITICAL,
                String.format("内存使用率过高: %.2f%% (阈值: %.2f%%)", usagePercentage, threshold),
                createMemoryDetails(usedMemory, totalMemory, usagePercentage)
            );
        } else {
            resolveAlert(ISpeechAlertingService.AlertType.HIGH_MEMORY_USAGE);
        }
    }
    
    /**
     * 检查并发连接数告警
     */
    private void checkConcurrentConnectionsAlert(ISpeechMetricsCollector.PerformanceSnapshot snapshot) {
        int threshold = performanceConfig.getAlerting().getConcurrentConnectionsThreshold();
        long currentConnections = snapshot.getCurrentConcurrentRequests();
        
        if (currentConnections >= threshold) {
            triggerAlert(ISpeechAlertingService.AlertType.HIGH_CONCURRENT_CONNECTIONS,
                ISpeechAlertingService.AlertSeverity.WARNING,
                String.format("并发连接数过高: %d (阈值: %d)", currentConnections, threshold),
                createConcurrentConnectionsDetails(snapshot, currentConnections)
            );
        } else {
            resolveAlert(ISpeechAlertingService.AlertType.HIGH_CONCURRENT_CONNECTIONS);
        }
    }
    
    /**
     * 触发告警
     */
    private void triggerAlert(ISpeechAlertingService.AlertType alertType, ISpeechAlertingService.AlertSeverity severity, String message, Map<String, Object> details) {
        AlertState currentState = alertStates.get(alertType);
        LocalDateTime now = LocalDateTime.now();
        
        // 检查冷却时间
        if (currentState != null && currentState.isActive()) {
            int cooldownMinutes = performanceConfig.getAlerting().getAlertCooldownMinutes();
            if (currentState.getLastTriggeredTime().plusMinutes(cooldownMinutes).isAfter(now)) {
                log.debug("[triggerAlert] 告警仍在冷却期内: alertType={}", alertType);
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
        ISpeechAlertingService.AlertRecord record = new ISpeechAlertingService.AlertRecord();
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
        
        log.warn("[triggerAlert] 触发告警: alertType={}, severity={}, message={}", alertType, severity, message);
        
        // 通知监听器
        notifyAlertListeners(record);
    }
    
    /**
     * 解决告警
     */
    private void resolveAlert(ISpeechAlertingService.AlertType alertType) {
        AlertState currentState = alertStates.get(alertType);
        
        if (currentState != null && currentState.isActive()) {
            currentState.setActive(false);
            currentState.setResolvedTime(LocalDateTime.now());
            
            log.info("[resolveAlert] 告警已解决: alertType={}", alertType);
            
            // 创建解决记录
            ISpeechAlertingService.AlertRecord record = new ISpeechAlertingService.AlertRecord();
            record.setAlertType(alertType);
            record.setSeverity(ISpeechAlertingService.AlertSeverity.INFO);
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
    private void notifyAlertListeners(ISpeechAlertingService.AlertRecord record) {
        for (ISpeechAlertingService.AlertListener listener : alertListeners) {
            try {
                listener.onAlert(record);
            } catch (Exception e) {
                log.error("[notifyAlertListeners] 通知告警监听器时发生错误: error={}", e.getMessage(), e);
            }
        }
    }
    
    @Override
    public void addAlertListener(ISpeechAlertingService.AlertListener listener) {
        log.debug("[addAlertListener] 添加告警监听器");
        alertListeners.add(listener);
    }
    
    @Override
    public void removeAlertListener(ISpeechAlertingService.AlertListener listener) {
        log.debug("[removeAlertListener] 移除告警监听器");
        alertListeners.remove(listener);
    }
    
    @Override
    public List<ISpeechAlertingService.AlertRecord> getActiveAlerts() {
        log.debug("[getActiveAlerts] 获取当前活跃告警");
        synchronized (alertHistoryLock) {
            return alertHistory.stream()
                    .filter(ISpeechAlertingService.AlertRecord::isActive)
                    .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                    .toList();
        }
    }
    
    @Override
    public List<ISpeechAlertingService.AlertRecord> getAlertHistory(int hours) {
        log.debug("[getAlertHistory] 获取告警历史: hours={}", hours);
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        
        synchronized (alertHistoryLock) {
            return alertHistory.stream()
                    .filter(record -> record.getTimestamp().isAfter(cutoff))
                    .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                    .toList();
        }
    }
    
    @Override
    public ISpeechAlertingService.AlertStatistics getAlertStatistics(int hours) {
        log.debug("[getAlertStatistics] 获取告警统计: hours={}", hours);
        List<ISpeechAlertingService.AlertRecord> records = getAlertHistory(hours);
        
        ISpeechAlertingService.AlertStatistics stats = new ISpeechAlertingService.AlertStatistics();
        stats.setTotalAlerts(records.size());
        stats.setActiveAlerts(records.stream().mapToInt(r -> r.isActive() ? 1 : 0).sum());
        
        Map<ISpeechAlertingService.AlertType, Long> alertsByType = new HashMap<>();
        Map<ISpeechAlertingService.AlertSeverity, Long> alertsBySeverity = new HashMap<>();
        
        for (ISpeechAlertingService.AlertRecord record : records) {
            alertsByType.merge(record.getAlertType(), 1L, Long::sum);
            alertsBySeverity.merge(record.getSeverity(), 1L, Long::sum);
        }
        
        stats.setAlertsByType(alertsByType);
        stats.setAlertsBySeverity(alertsBySeverity);
        
        return stats;
    }
    
    // 创建详情方法
    private Map<String, Object> createFailureRateDetails(ISpeechMetricsCollector.PerformanceSnapshot snapshot, double currentRate) {
        Map<String, Object> details = new HashMap<>();
        details.put("currentFailureRate", currentRate);
        details.put("threshold", performanceConfig.getAlerting().getFailureRateThreshold());
        details.put("totalRequests", snapshot.getTotalRequests());
        details.put("failedRequests", snapshot.getFailedRequests());
        details.put("errorCounts", snapshot.getErrorCounts());
        return details;
    }
    
    private Map<String, Object> createLatencyDetails(ISpeechMetricsCollector.PerformanceSnapshot snapshot, double currentLatency) {
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
    
    private Map<String, Object> createConcurrentConnectionsDetails(ISpeechMetricsCollector.PerformanceSnapshot snapshot, long currentConnections) {
        Map<String, Object> details = new HashMap<>();
        details.put("currentConnections", currentConnections);
        details.put("threshold", performanceConfig.getAlerting().getConcurrentConnectionsThreshold());
        details.put("maxConnections", snapshot.getMaxConcurrentRequests());
        return details;
    }
    
    /**
     * 告警状态（内部使用）
     */
    @Data
    private static class AlertState {
        private boolean active;
        private LocalDateTime firstTriggeredTime;
        private LocalDateTime lastTriggeredTime;
        private LocalDateTime resolvedTime;
        private int triggerCount;
    }
}
