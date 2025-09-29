package com.carol.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

/**
 * TTS语音合成请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TtsSynthesisRequest {
    
    /**
     * 要合成的文本内容（必填）
     */
    @NotBlank(message = "文本内容不能为空")
    @Size(max = 600, message = "文本长度不能超过600字符")
    private String text;
    
    /**
     * 音色选择（可选，默认为Cherry）
     * Qwen3-TTS支持的音色：Cherry, Ethan, Nofish, Jennifer, Ryan, Katerina, Elias, 
     * Jada, Dylan, Sunny, li, Marcus, Roy, Peter, Rocky, Kiki, Eric
     */
    @Pattern(regexp = "^(Cherry|Ethan|Nofish|Jennifer|Ryan|Katerina|Elias|Jada|Dylan|Sunny|li|Marcus|Roy|Peter|Rocky|Kiki|Eric|Serena|Chelsie)$", 
             message = "不支持的音色类型")
    private String voice = "Cherry";
    
    /**
     * 语言类型（可选，默认为Chinese）
     * 支持：Chinese, English, Spanish, Russian, Italian, French, Korean, Japanese, German, Portuguese
     */
    @Pattern(regexp = "^(Chinese|English|Spanish|Russian|Italian|French|Korean|Japanese|German|Portuguese)$", 
             message = "不支持的语言类型")
    private String languageType = "Chinese";
    
    /**
     * 使用的TTS模型（可选，默认为qwen3-tts-flash）
     */
    @Pattern(regexp = "^(qwen3-tts-flash|qwen-tts|qwen-tts-latest)$", 
             message = "不支持的TTS模型")
    private String model = "qwen3-tts-flash";
    
    /**
     * 是否流式输出（可选，默认为false）
     */
    private Boolean stream = false;
    
    /**
     * 角色ID（可选，用于角色语音合成）
     */
    private Long characterId;
    
    /**
     * 用户ID（用于日志追踪）
     */
    private String userId;
    
    /**
     * 是否保存音频文件到本地（可选，默认为false）
     */
    private Boolean saveToLocal = false;
    
    /**
     * 自定义文件名（可选）
     */
    private String fileName;
}
