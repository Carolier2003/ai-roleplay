package com.carol.backend.service.impl;

import com.carol.backend.config.SpeechPerformanceConfig;
import com.carol.backend.service.ISpeechMetricsCollector;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 语音识别性能指标收集器实现类
 * 
 * @author jianjl
 * @version 1.0
 * @description 语音识别性能指标收集器实现
 * @date 2025-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechMetricsCollectorImpl implements ISpeechMetricsCollector {
    
    private final SpeechPerformanceConfig performanceConfig;
    
    // 计数器
    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder successfulRequests = new LongAdder();
    private final LongAdder failedRequests = new LongAdder();
    
    // 延迟统计
    private final AtomicLong totalLatency = new AtomicLong(0);
    private final AtomicLong maxLatency = new AtomicLong(0);
    private final AtomicLong minLatency = new AtomicLong(Long.MAX_VALUE);
    
    // 并发统计
    private final AtomicLong currentConcurrentRequests = new AtomicLong(0);
    private final AtomicLong maxConcurrentRequests = new AtomicLong(0);
    
    // 文件大小统计
    private final AtomicLong totalFileSize = new AtomicLong(0);
    private final AtomicLong maxFileSize = new AtomicLong(0);
    
    // 错误统计
    private final Map<String, LongAdder> errorCounts = new ConcurrentHashMap<>();
    
    // 最近的请求详情（用于计算移动平均）
    private final List<RequestMetric> recentRequests = new ArrayList<>();
    private final Object recentRequestsLock = new Object();
    
    // 历史数据
    private final List<ISpeechMetricsCollector.PerformanceSnapshot> historicalData = new ArrayList<>();
    private final AtomicReference<ISpeechMetricsCollector.PerformanceSnapshot> currentSnapshot = new AtomicReference<>();
    
    @Override
    public ISpeechMetricsCollector.RequestContext recordRequestStart(String requestType, long fileSize) {
        log.debug("[recordRequestStart] 记录请求开始: requestType={}, fileSize={}", requestType, fileSize);
        
        totalRequests.increment();
        long concurrentCount = currentConcurrentRequests.incrementAndGet();
        
        // 更新最大并发数
        maxConcurrentRequests.updateAndGet(max -> Math.max(max, concurrentCount));
        
        // 更新文件大小统计
        if (fileSize > 0) {
            totalFileSize.addAndGet(fileSize);
            maxFileSize.updateAndGet(max -> Math.max(max, fileSize));
        }
        
        ISpeechMetricsCollector.RequestContext context = new ISpeechMetricsCollector.RequestContext();
        context.setRequestType(requestType);
        context.setStartTime(System.currentTimeMillis());
        context.setFileSize(fileSize);
        
        log.debug("[recordRequestStart] 请求开始记录完成: requestType={}, fileSize={}, concurrent={}", 
                requestType, fileSize, concurrentCount);
        
        return context;
    }
    
    @Override
    public void recordRequestComplete(ISpeechMetricsCollector.RequestContext context, boolean success, String errorType) {
        long endTime = System.currentTimeMillis();
        long latency = endTime - context.getStartTime();
        
        currentConcurrentRequests.decrementAndGet();
        
        if (success) {
            successfulRequests.increment();
        } else {
            failedRequests.increment();
            if (errorType != null) {
                errorCounts.computeIfAbsent(errorType, k -> new LongAdder()).increment();
            }
        }
        
        // 更新延迟统计
        totalLatency.addAndGet(latency);
        maxLatency.updateAndGet(max -> Math.max(max, latency));
        minLatency.updateAndGet(min -> Math.min(min, latency));
        
        // 记录最近请求
        RequestMetric metric = new RequestMetric();
        metric.setRequestType(context.getRequestType());
        metric.setLatency(latency);
        metric.setFileSize(context.getFileSize());
        metric.setSuccess(success);
        metric.setErrorType(errorType);
        metric.setTimestamp(LocalDateTime.now());
        
        synchronized (recentRequestsLock) {
            recentRequests.add(metric);
            
            // 保持最近1000个请求
            if (recentRequests.size() > 1000) {
                recentRequests.remove(0);
            }
        }
    }
    
    @Override
    public ISpeechMetricsCollector.PerformanceSnapshot getCurrentMetrics() {
        log.debug("[getCurrentMetrics] 获取当前性能快照");
        
        long total = totalRequests.sum();
        long successful = successfulRequests.sum();
        long failed = failedRequests.sum();
        
        ISpeechMetricsCollector.PerformanceSnapshot snapshot = new ISpeechMetricsCollector.PerformanceSnapshot();
        snapshot.setTimestamp(LocalDateTime.now());
        snapshot.setTotalRequests(total);
        snapshot.setSuccessfulRequests(successful);
        snapshot.setFailedRequests(failed);
        snapshot.setSuccessRate(total > 0 ? (double) successful / total * 100 : 0);
        snapshot.setFailureRate(total > 0 ? (double) failed / total * 100 : 0);
        
        // 延迟统计
        if (successful > 0) {
            snapshot.setAverageLatency((double) totalLatency.get() / total);
            snapshot.setMaxLatency(maxLatency.get());
            snapshot.setMinLatency(minLatency.get() == Long.MAX_VALUE ? 0 : minLatency.get());
        }
        
        // 并发统计
        snapshot.setCurrentConcurrentRequests(currentConcurrentRequests.get());
        snapshot.setMaxConcurrentRequests(maxConcurrentRequests.get());
        
        // 文件大小统计
        snapshot.setTotalFileSize(totalFileSize.get());
        snapshot.setMaxFileSize(maxFileSize.get());
        snapshot.setAverageFileSize(total > 0 ? (double) totalFileSize.get() / total : 0);
        
        // 错误统计
        Map<String, Long> errors = errorCounts.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().sum()
                ));
        snapshot.setErrorCounts(errors);
        
        // 计算最近请求的统计
        calculateRecentMetrics(snapshot);
        
        return snapshot;
    }
    
    /**
     * 计算最近请求的统计
     */
    private void calculateRecentMetrics(ISpeechMetricsCollector.PerformanceSnapshot snapshot) {
        synchronized (recentRequestsLock) {
            if (recentRequests.isEmpty()) {
                return;
            }
            
            // 最近5分钟的请求
            LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
            List<RequestMetric> recentRequests5min = recentRequests.stream()
                    .filter(r -> r.getTimestamp().isAfter(fiveMinutesAgo))
                    .collect(Collectors.toList());
            
            if (!recentRequests5min.isEmpty()) {
                long recentSuccessful = recentRequests5min.stream()
                        .mapToLong(r -> r.isSuccess() ? 1 : 0)
                        .sum();
                
                double recentSuccessRate = (double) recentSuccessful / recentRequests5min.size() * 100;
                snapshot.setRecentSuccessRate(recentSuccessRate);
                
                double recentAvgLatency = recentRequests5min.stream()
                        .mapToLong(RequestMetric::getLatency)
                        .average()
                        .orElse(0.0);
                snapshot.setRecentAverageLatency(recentAvgLatency);
            }
        }
    }
    
    /**
     * 定期保存性能快照
     */
    @Scheduled(fixedRateString = "#{${app.speech.performance.monitoring.metrics-interval-seconds:60} * 1000}")
    public void savePerformanceSnapshot() {
        if (!performanceConfig.getMonitoring().isEnabled()) {
            return;
        }
        
        ISpeechMetricsCollector.PerformanceSnapshot snapshot = getCurrentMetrics();
        currentSnapshot.set(snapshot);
        
        synchronized (historicalData) {
            historicalData.add(snapshot);
            
            // 清理过期数据
            int retentionHours = performanceConfig.getMonitoring().getDataRetentionHours();
            LocalDateTime cutoff = LocalDateTime.now().minusHours(retentionHours);
            historicalData.removeIf(data -> data.getTimestamp().isBefore(cutoff));
        }
        
        log.info("[savePerformanceSnapshot] 性能快照已保存: totalRequests={}, successRate={}%, averageLatency={}ms, currentConcurrentRequests={}", 
                snapshot.getTotalRequests(), 
                String.format("%.2f", snapshot.getSuccessRate()), 
                String.format("%.2f", snapshot.getAverageLatency()),
                snapshot.getCurrentConcurrentRequests());
    }
    
    @Override
    public List<ISpeechMetricsCollector.PerformanceSnapshot> getHistoricalData(int hours) {
        log.debug("[getHistoricalData] 获取历史性能数据: hours={}", hours);
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        
        synchronized (historicalData) {
            return historicalData.stream()
                    .filter(data -> data.getTimestamp().isAfter(cutoff))
                    .collect(Collectors.toList());
        }
    }
    
    @Override
    public void resetMetrics() {
        log.info("[resetMetrics] 重置统计数据");
        totalRequests.reset();
        successfulRequests.reset();
        failedRequests.reset();
        totalLatency.set(0);
        maxLatency.set(0);
        minLatency.set(Long.MAX_VALUE);
        currentConcurrentRequests.set(0);
        maxConcurrentRequests.set(0);
        totalFileSize.set(0);
        maxFileSize.set(0);
        errorCounts.clear();
        
        synchronized (recentRequestsLock) {
            recentRequests.clear();
        }
        
        synchronized (historicalData) {
            historicalData.clear();
        }
        
        log.info("[resetMetrics] 性能指标重置完成");
    }
    
    /**
     * 请求指标（内部使用）
     */
    @Data
    private static class RequestMetric {
        private String requestType;
        private long latency;
        private long fileSize;
        private boolean success;
        private String errorType;
        private LocalDateTime timestamp;
    }
}
