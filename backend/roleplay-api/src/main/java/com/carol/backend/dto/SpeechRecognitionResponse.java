package com.carol.backend.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 语音识别响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeechRecognitionResponse {
    
    /**
     * 请求ID
     */
    private String requestId;
    
    /**
     * 识别文本
     */
    private String text;
    
    /**
     * 识别置信度 (0.0-1.0)
     */
    private Double confidence;
    
    /**
     * 是否为完整句子(产生断句)
     */
    private Boolean isSentenceEnd;
    
    /**
     * 句子开始时间(ms)
     */
    private Long beginTime;
    
    /**
     * 句子结束时间(ms)
     */
    private Long endTime;
    
    /**
     * 字级别时间戳信息
     */
    private List<WordTimestamp> words;
    
    /**
     * 首包延迟(ms)
     */
    private Long firstPackageDelay;
    
    /**
     * 尾包延迟(ms)
     */
    private Long lastPackageDelay;
    
    /**
     * 音频时长（秒）
     */
    private Double duration;
    
    /**
     * 处理时间戳
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * 字时间戳信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WordTimestamp {
        /**
         * 字开始时间(ms)
         */
        private Long beginTime;
        
        /**
         * 字结束时间(ms)
         */
        private Long endTime;
        
        /**
         * 字内容
         */
        private String text;
        
        /**
         * 标点符号
         */
        private String punctuation;
    }
}
