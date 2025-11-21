package com.carol.backend.service;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 语音识别性能指标收集器接口
 * 
 * @author jianjl
 * @version 1.0
 * @description 语音识别性能指标收集器
 * @date 2025-01-15
 */
public interface ISpeechMetricsCollector {
    
    /**
     * 记录请求开始
     * 
     * @param requestType 请求类型
     * @param fileSize 文件大小
     * @return 请求上下文
     */
    RequestContext recordRequestStart(String requestType, long fileSize);
    
    /**
     * 记录请求完成
     * 
     * @param context 请求上下文
     * @param success 是否成功
     * @param errorType 错误类型
     */
    void recordRequestComplete(RequestContext context, boolean success, String errorType);
    
    /**
     * 获取当前性能快照
     * 
     * @return 性能快照
     */
    PerformanceSnapshot getCurrentMetrics();
    
    /**
     * 获取历史性能数据
     * 
     * @param hours 小时数
     * @return 历史数据列表
     */
    List<PerformanceSnapshot> getHistoricalData(int hours);
    
    /**
     * 重置统计数据
     */
    void resetMetrics();
    
    /**
     * 请求上下文
     */
    @Data
    class RequestContext {
        private String requestType;
        private long startTime;
        private long fileSize;
    }
    
    /**
     * 性能快照
     */
    @Data
    class PerformanceSnapshot {
        private LocalDateTime timestamp;
        
        // 请求统计
        private long totalRequests;
        private long successfulRequests;
        private long failedRequests;
        private double successRate;
        private double failureRate;
        
        // 延迟统计
        private double averageLatency;
        private long maxLatency;
        private long minLatency;
        
        // 并发统计
        private long currentConcurrentRequests;
        private long maxConcurrentRequests;
        
        // 文件统计
        private long totalFileSize;
        private long maxFileSize;
        private double averageFileSize;
        
        // 错误统计
        private Map<String, Long> errorCounts;
        
        // 最近统计（5分钟内）
        private double recentSuccessRate;
        private double recentAverageLatency;
    }
}
