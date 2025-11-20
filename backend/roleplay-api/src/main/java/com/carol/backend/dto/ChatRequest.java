package com.carol.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 聊天请求DTO
 * ✅ 已移除 userId 和 conversationId 字段，完全从 JWT 中获取用户信息
 * 
 * @author carol
 */
@Data
public class ChatRequest {
    
    /**
     * 用户消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    private String message;
    
    /**
     * 角色ID（可选，用于角色扮演）
     */
    private Long characterId;
    
    /**
     * 是否启用语音合成（可选，默认false）
     */
    private Boolean enableTts = false;
    
    /**
     * 指定音色（可选，不指定则使用角色默认音色或系统默认）
     */
    private String voice;
    
    /**
     * 语言类型（可选，系统自动检测）
     */
    private String languageType;
    
    /**
     * 是否启用RAG知识检索（可选，默认true）
     * 用于测试对比RAG增强效果
     */
    private Boolean enableRag = true;

}
