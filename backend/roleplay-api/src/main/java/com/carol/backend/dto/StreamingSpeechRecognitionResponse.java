package com.carol.backend.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 流式语音识别响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamingSpeechRecognitionResponse {
    
    /**
     * 响应类型
     */
    private ResponseType type;
    
    /**
     * 请求ID
     */
    private String requestId;
    
    /**
     * 识别结果
     */
    private SpeechRecognitionResponse.WordTimestamp result;
    
    /**
     * 错误信息
     */
    private String error;
    
    /**
     * 时间戳
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * 响应类型枚举
     */
    public enum ResponseType {
        /**
         * 识别结果事件
         */
        RESULT,
        
        /**
         * 识别完成事件
         */
        COMPLETE,
        
        /**
         * 错误事件
         */
        ERROR,
        
        /**
         * 连接建立事件
         */
        CONNECTED,
        
        /**
         * 连接关闭事件
         */
        CLOSED
    }
    
    /**
     * 创建结果响应
     */
    public static StreamingSpeechRecognitionResponse result(String requestId, SpeechRecognitionResponse.WordTimestamp result) {
        return StreamingSpeechRecognitionResponse.builder()
                .type(ResponseType.RESULT)
                .requestId(requestId)
                .result(result)
                .build();
    }
    
    /**
     * 创建完成响应
     */
    public static StreamingSpeechRecognitionResponse complete(String requestId) {
        return StreamingSpeechRecognitionResponse.builder()
                .type(ResponseType.COMPLETE)
                .requestId(requestId)
                .build();
    }
    
    /**
     * 创建错误响应
     */
    public static StreamingSpeechRecognitionResponse error(String requestId, String error) {
        return StreamingSpeechRecognitionResponse.builder()
                .type(ResponseType.ERROR)
                .requestId(requestId)
                .error(error)
                .build();
    }
    
    /**
     * 创建连接建立响应
     */
    public static StreamingSpeechRecognitionResponse connected(String requestId) {
        return StreamingSpeechRecognitionResponse.builder()
                .type(ResponseType.CONNECTED)
                .requestId(requestId)
                .build();
    }
    
    /**
     * 创建连接关闭响应
     */
    public static StreamingSpeechRecognitionResponse closed(String requestId) {
        return StreamingSpeechRecognitionResponse.builder()
                .type(ResponseType.CLOSED)
                .requestId(requestId)
                .build();
    }
}
