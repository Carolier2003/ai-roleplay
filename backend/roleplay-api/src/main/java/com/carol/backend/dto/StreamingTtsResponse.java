package com.carol.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 流式TTS语音合成响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StreamingTtsResponse {
    
    /**
     * 响应类型（chunk, final, error）
     */
    private String type;
    
    /**
     * Base64编码的音频数据（PCM格式）
     */
    private String audioData;
    
    /**
     * 是否为最后一个数据包
     */
    private Boolean isFinished;
    
    /**
     * 当前数据包序号
     */
    private Integer sequenceNumber;
    
    /**
     * 音频时长（当前数据包，秒）
     */
    private Double chunkDuration;
    
    /**
     * 累计音频时长（秒）
     */
    private Double totalDuration;
    
    /**
     * 完整音频URL（仅在最后一个包中提供）
     */
    private String completeAudioUrl;
    
    /**
     * 当前时间戳
     */
    private LocalDateTime timestamp;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 错误信息（如果有错误）
     */
    private String errorMessage;
    
    /**
     * 错误代码（如果有错误）
     */
    private String errorCode;
    
    /**
     * 使用的音色
     */
    private String voice;
    
    /**
     * 使用的语言类型
     */
    private String languageType;
    
    /**
     * 处理状态（processing, completed, error）
     */
    private String status;
    
    /**
     * 创建音频数据包响应
     */
    public static StreamingTtsResponse createChunk(String audioData, Integer sequenceNumber, 
                                                   Double chunkDuration, Double totalDuration, String sessionId) {
        return StreamingTtsResponse.builder()
                .type("chunk")
                .audioData(audioData)
                .isFinished(false)
                .sequenceNumber(sequenceNumber)
                .chunkDuration(chunkDuration)
                .totalDuration(totalDuration)
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .status("processing")
                .build();
    }
    
    /**
     * 创建最终响应
     */
    public static StreamingTtsResponse createFinal(String completeAudioUrl, Double totalDuration, String sessionId) {
        return StreamingTtsResponse.builder()
                .type("final")
                .isFinished(true)
                .completeAudioUrl(completeAudioUrl)
                .totalDuration(totalDuration)
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .status("completed")
                .build();
    }
    
    /**
     * 创建错误响应
     */
    public static StreamingTtsResponse createError(String errorMessage, String errorCode, String sessionId) {
        return StreamingTtsResponse.builder()
                .type("error")
                .isFinished(true)
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .status("error")
                .build();
    }
}
