package com.carol.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * 语音识别配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.speech.recognition")
public class SpeechRecognitionConfig {
    
    /**
     * 默认模型
     */
    private String defaultModel = "fun-asr-realtime";
    
    /**
     * 默认采样率
     */
    private Integer defaultSampleRate = 16000;
    
    /**
     * 默认音频格式
     */
    private String defaultFormat = "wav";
    
    /**
     * 是否开启语义断句
     */
    private Boolean semanticPunctuationEnabled = false;
    
    /**
     * 是否开启标点预测
     */
    private Boolean punctuationPredictionEnabled = true;
    
    /**
     * VAD断句静音时长阈值(ms)
     */
    private Integer maxSentenceSilence = 1300;
    
    /**
     * 是否开启心跳保持长连接
     */
    private Boolean heartbeat = false;
    
    /**
     * 语言提示(仅paraformer-realtime-v2模型支持)
     */
    private String[] languageHints = {"zh", "en"};
    
    /**
     * 支持的音频格式
     */
    private String[] supportedFormats = {"pcm", "wav", "mp3", "opus", "speex", "aac", "amr"};
    
    /**
     * 最大音频文件大小(MB)
     */
    private Integer maxFileSizeMB = 100;
    
    /**
     * 流式识别每包音频时长(ms)
     */
    private Integer streamChunkDurationMs = 100;
    
    /**
     * 流式识别每包数据大小范围(KB)
     */
    private Integer minChunkSizeKB = 1;
    private Integer maxChunkSizeKB = 16;
}
