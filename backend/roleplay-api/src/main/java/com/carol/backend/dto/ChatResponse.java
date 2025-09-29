package com.carol.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 聊天响应DTO
 * 
 * @author carol
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {
    
    /**
     * AI回复内容
     */
    private String content;
    
    /**
     * 会话ID
     */
    private String conversationId;
    
    /**
     * 角色信息（可选）
     */
    private CharacterInfo character;
    
    /**
     * 响应时间戳
     */
    private LocalDateTime timestamp;
    
    /**
     * TTS语音合成信息（可选）
     */
    private AudioInfo audio;
    
    /**
     * 构造简单响应
     */
    public ChatResponse(String content, String conversationId) {
        this.content = content;
        this.conversationId = conversationId;
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * 构造完整响应（保持向后兼容）
     */
    public ChatResponse(String content, String conversationId, CharacterInfo character, LocalDateTime timestamp) {
        this.content = content;
        this.conversationId = conversationId;
        this.character = character;
        this.timestamp = timestamp;
    }
    
    /**
     * 角色信息内部类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CharacterInfo {
        private Long id;
        private String name;
        private String avatar;
        private String voice;  // 角色使用的音色
    }
    
    /**
     * 音频信息内部类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AudioInfo {
        /**
         * 音频下载URL（24小时有效）
         */
        private String audioUrl;
        
        /**
         * 音频时长（秒）
         */
        private Double duration;
        
        /**
         * 使用的音色
         */
        private String voice;
        
        /**
         * 语言类型
         */
        private String languageType;
        
        /**
         * 合成是否成功
         */
        private Boolean success;
        
        /**
         * 错误信息（如果失败）
         */
        private String errorMessage;
        
        /**
         * 文本字符数
         */
        private Integer characterCount;
        
        /**
         * 估算费用（元）
         */
        private Double estimatedCost;
        
        /**
         * 处理耗时（毫秒）
         */
        private Long processingTime;
    }
}
