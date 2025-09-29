package com.carol.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * TTS语音合成响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TtsSynthesisResponse {
    
    /**
     * 合成成功标识
     */
    private Boolean success;
    
    /**
     * 音频下载URL（24小时有效）
     */
    private String audioUrl;
    
    /**
     * 音频时长（秒）
     */
    private Double duration;
    
    /**
     * 音频格式
     */
    private String format;
    
    /**
     * 采样率
     */
    private Integer sampleRate;
    
    /**
     * 使用的音色
     */
    private String voice;
    
    /**
     * 使用的语言类型
     */
    private String languageType;
    
    /**
     * 使用的模型
     */
    private String model;
    
    /**
     * 文本字符数
     */
    private Integer characterCount;
    
    /**
     * 请求ID（用于追踪）
     */
    private String requestId;
    
    /**
     * 合成开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 合成完成时间
     */
    private LocalDateTime endTime;
    
    /**
     * 处理耗时（毫秒）
     */
    private Long processingTime;
    
    /**
     * 本地文件路径（如果保存到本地）
     */
    private String localFilePath;
    
    /**
     * 错误信息（如果失败）
     */
    private String errorMessage;
    
    /**
     * Token消耗数量
     */
    private Integer tokenUsage;
    
    /**
     * 费用估算（元）
     */
    private Double estimatedCost;
}
