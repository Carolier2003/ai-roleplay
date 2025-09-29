package com.carol.backend.controller;

import com.carol.backend.dto.ApiResponse;
import com.carol.backend.service.SpeechAlertingService;
import com.carol.backend.service.SpeechMetricsCollector;
import com.carol.backend.service.SpeechResourceManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 语音识别监控控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/speech/monitoring")
@RequiredArgsConstructor
public class SpeechMonitoringController {
    
    private final SpeechMetricsCollector metricsCollector;
    private final SpeechAlertingService alertingService;
    private final SpeechResourceManager resourceManager;
    
    /**
     * 获取当前性能指标
     */
    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<SpeechMetricsCollector.PerformanceSnapshot>> getCurrentMetrics() {
        log.info("获取当前性能指标");
        
        try {
            SpeechMetricsCollector.PerformanceSnapshot snapshot = metricsCollector.getCurrentMetrics();
            return ResponseEntity.ok(ApiResponse.success(snapshot, "当前性能指标"));
        } catch (Exception e) {
            log.error("获取性能指标失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取性能指标失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取历史性能数据
     */
    @GetMapping("/metrics/history")
    public ResponseEntity<ApiResponse<Object>> getHistoricalMetrics(
            @RequestParam(defaultValue = "24") int hours) {
        log.info("获取历史性能数据: hours={}", hours);
        
        try {
            var historicalData = metricsCollector.getHistoricalData(hours);
            
            Map<String, Object> response = new HashMap<>();
            response.put("hours", hours);
            response.put("dataPoints", historicalData.size());
            response.put("data", historicalData);
            
            return ResponseEntity.ok(ApiResponse.success(response, "历史性能数据"));
        } catch (Exception e) {
            log.error("获取历史性能数据失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取历史性能数据失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取活跃告警
     */
    @GetMapping("/alerts/active")
    public ResponseEntity<ApiResponse<Object>> getActiveAlerts() {
        log.info("获取活跃告警");
        
        try {
            var activeAlerts = alertingService.getActiveAlerts();
            
            Map<String, Object> response = new HashMap<>();
            response.put("count", activeAlerts.size());
            response.put("alerts", activeAlerts);
            
            return ResponseEntity.ok(ApiResponse.success(response, "活跃告警列表"));
        } catch (Exception e) {
            log.error("获取活跃告警失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取活跃告警失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取告警历史
     */
    @GetMapping("/alerts/history")
    public ResponseEntity<ApiResponse<Object>> getAlertHistory(
            @RequestParam(defaultValue = "24") int hours) {
        log.info("获取告警历史: hours={}", hours);
        
        try {
            var alertHistory = alertingService.getAlertHistory(hours);
            
            Map<String, Object> response = new HashMap<>();
            response.put("hours", hours);
            response.put("count", alertHistory.size());
            response.put("alerts", alertHistory);
            
            return ResponseEntity.ok(ApiResponse.success(response, "告警历史"));
        } catch (Exception e) {
            log.error("获取告警历史失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取告警历史失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取告警统计
     */
    @GetMapping("/alerts/statistics")
    public ResponseEntity<ApiResponse<SpeechAlertingService.AlertStatistics>> getAlertStatistics(
            @RequestParam(defaultValue = "24") int hours) {
        log.info("获取告警统计: hours={}", hours);
        
        try {
            SpeechAlertingService.AlertStatistics statistics = alertingService.getAlertStatistics(hours);
            return ResponseEntity.ok(ApiResponse.success(statistics, "告警统计"));
        } catch (Exception e) {
            log.error("获取告警统计失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取告警统计失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取系统健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Object>> getSystemHealth() {
        log.info("获取系统健康状态");
        
        try {
            SpeechMetricsCollector.PerformanceSnapshot metrics = metricsCollector.getCurrentMetrics();
            var activeAlerts = alertingService.getActiveAlerts();
            
            // 计算健康分数
            int healthScore = calculateHealthScore(metrics, activeAlerts.size());
            String healthStatus = getHealthStatus(healthScore);
            
            Map<String, Object> response = new HashMap<>();
            response.put("healthScore", healthScore);
            response.put("healthStatus", healthStatus);
            response.put("activeAlertsCount", activeAlerts.size());
            response.put("successRate", metrics.getSuccessRate());
            response.put("averageLatency", metrics.getAverageLatency());
            response.put("currentConcurrentRequests", metrics.getCurrentConcurrentRequests());
            
            // 内存信息
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double memoryUsage = (double) usedMemory / totalMemory * 100;
            
            Map<String, Object> memoryInfo = new HashMap<>();
            memoryInfo.put("totalMB", totalMemory / (1024 * 1024));
            memoryInfo.put("usedMB", usedMemory / (1024 * 1024));
            memoryInfo.put("freeMB", freeMemory / (1024 * 1024));
            memoryInfo.put("usagePercentage", memoryUsage);
            
            response.put("memory", memoryInfo);
            
            return ResponseEntity.ok(ApiResponse.success(response, "系统健康状态"));
        } catch (Exception e) {
            log.error("获取系统健康状态失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取系统健康状态失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取资源使用情况
     */
    @GetMapping("/resources")
    public ResponseEntity<ApiResponse<SpeechResourceManager.ResourceUsageStats>> getResourceUsage() {
        log.info("获取资源使用情况");
        
        try {
            SpeechResourceManager.ResourceUsageStats stats = resourceManager.getResourceUsageStats();
            return ResponseEntity.ok(ApiResponse.success(stats, "资源使用情况"));
        } catch (Exception e) {
            log.error("获取资源使用情况失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取资源使用情况失败: " + e.getMessage()));
        }
    }
    
    /**
     * 强制垃圾回收
     */
    @PostMapping("/gc")
    public ResponseEntity<ApiResponse<String>> forceGarbageCollection() {
        log.info("执行强制垃圾回收");
        
        try {
            resourceManager.forceGC();
            return ResponseEntity.ok(ApiResponse.success("垃圾回收已执行"));
        } catch (Exception e) {
            log.error("执行垃圾回收失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("执行垃圾回收失败: " + e.getMessage()));
        }
    }
    
    /**
     * 重置性能指标
     */
    @PostMapping("/metrics/reset")
    public ResponseEntity<ApiResponse<String>> resetMetrics() {
        log.info("重置性能指标");
        
        try {
            metricsCollector.resetMetrics();
            return ResponseEntity.ok(ApiResponse.success("性能指标已重置"));
        } catch (Exception e) {
            log.error("重置性能指标失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("重置性能指标失败: " + e.getMessage()));
        }
    }
    
    /**
     * 计算健康分数 (0-100)
     */
    private int calculateHealthScore(SpeechMetricsCollector.PerformanceSnapshot metrics, int activeAlertsCount) {
        int score = 100;
        
        // 根据成功率扣分
        if (metrics.getSuccessRate() < 95) {
            score -= (int) ((95 - metrics.getSuccessRate()) * 2);
        }
        
        // 根据延迟扣分
        if (metrics.getAverageLatency() > 5000) {
            score -= Math.min(30, (int) ((metrics.getAverageLatency() - 5000) / 1000 * 5));
        }
        
        // 根据活跃告警扣分
        score -= activeAlertsCount * 10;
        
        return Math.max(0, score);
    }
    
    /**
     * 获取健康状态描述
     */
    private String getHealthStatus(int healthScore) {
        if (healthScore >= 90) {
            return "优秀";
        } else if (healthScore >= 70) {
            return "良好";
        } else if (healthScore >= 50) {
            return "一般";
        } else {
            return "差";
        }
    }
}
