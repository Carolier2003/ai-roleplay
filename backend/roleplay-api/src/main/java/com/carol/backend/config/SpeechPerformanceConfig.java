package com.carol.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * 语音识别性能配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.speech.performance")
public class SpeechPerformanceConfig {
    
    /**
     * 音频文件限制
     */
    private FileLimit fileLimit = new FileLimit();
    
    /**
     * 超时配置
     */
    private Timeout timeout = new Timeout();
    
    /**
     * 并发限制
     */
    private Concurrency concurrency = new Concurrency();
    
    /**
     * 监控配置
     */
    private Monitoring monitoring = new Monitoring();
    
    /**
     * 告警配置
     */
    private Alerting alerting = new Alerting();
    
    @Data
    public static class FileLimit {
        /**
         * 最大文件大小 (MB)
         */
        private long maxFileSizeMB = 50;
        
        /**
         * 最大音频时长 (秒)
         */
        private int maxDurationSeconds = 300;
        
        /**
         * 允许的音频格式
         */
        private String[] allowedFormats = {"wav", "mp3", "pcm", "opus", "speex", "aac", "amr"};
        
        /**
         * 最大采样率
         */
        private int maxSampleRate = 48000;
        
        /**
         * 最小采样率
         */
        private int minSampleRate = 8000;
    }
    
    @Data
    public static class Timeout {
        /**
         * 同步识别超时时间 (秒)
         */
        private int syncRecognitionTimeoutSeconds = 60;
        
        /**
         * 异步识别超时时间 (秒)
         */
        private int asyncRecognitionTimeoutSeconds = 300;
        
        /**
         * 流式识别会话超时时间 (秒)
         */
        private int streamingSessionTimeoutSeconds = 300;
        
        /**
         * 连接超时时间 (秒)
         */
        private int connectionTimeoutSeconds = 30;
        
        /**
         * 读取超时时间 (秒)
         */
        private int readTimeoutSeconds = 60;
    }
    
    @Data
    public static class Concurrency {
        /**
         * 最大并发同步识别任务数
         */
        private int maxSyncTasks = 10;
        
        /**
         * 最大并发异步识别任务数
         */
        private int maxAsyncTasks = 20;
        
        /**
         * 最大并发流式会话数
         */
        private int maxStreamingSessions = 50;
        
        /**
         * 队列大小
         */
        private int queueSize = 100;
        
        /**
         * 核心线程数
         */
        private int corePoolSize = 5;
        
        /**
         * 最大线程数
         */
        private int maxPoolSize = 20;
    }
    
    @Data
    public static class Monitoring {
        /**
         * 是否启用监控
         */
        private boolean enabled = true;
        
        /**
         * 指标收集间隔 (秒)
         */
        private int metricsIntervalSeconds = 60;
        
        /**
         * 性能数据保留时间 (小时)
         */
        private int dataRetentionHours = 24;
        
        /**
         * 是否记录详细请求日志
         */
        private boolean enableDetailedLogging = true;
        
        /**
         * 慢请求阈值 (毫秒)
         */
        private long slowRequestThresholdMs = 5000;
    }
    
    @Data
    public static class Alerting {
        /**
         * 是否启用告警
         */
        private boolean enabled = true;
        
        /**
         * 失败率告警阈值 (%)
         */
        private double failureRateThreshold = 10.0;
        
        /**
         * 平均延迟告警阈值 (毫秒)
         */
        private long avgLatencyThresholdMs = 10000;
        
        /**
         * 内存使用率告警阈值 (%)
         */
        private double memoryUsageThreshold = 80.0;
        
        /**
         * 并发连接数告警阈值
         */
        private int concurrentConnectionsThreshold = 80;
        
        /**
         * 告警检查间隔 (分钟)
         */
        private int alertCheckIntervalMinutes = 5;
        
        /**
         * 告警冷却时间 (分钟)
         */
        private int alertCooldownMinutes = 15;
    }
}
