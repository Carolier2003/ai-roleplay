package com.carol.backend.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;

/**
 * 更新语音时长请求参数
 * 
 * @author carol
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVoiceDurationRequest {
    
    /**
     * 会话ID（可选，如果不提供会根据characterId和userId生成）
     */
    private String conversationId;
    
    /**
     * 消息内容（用于定位消息）
     */
    @NotBlank(message = "消息内容不能为空")
    private String messageContent;
    
    /**
     * 语音时长（秒）
     */
    @NotNull(message = "语音时长不能为空")
    @Min(value = 0, message = "语音时长不能为负数")
    private Integer voiceDuration;
    
    /**
     * 角色ID（可选，用于生成会话ID）
     */
    private Long characterId;
}
