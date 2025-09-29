package com.carol.backend.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import java.util.Map;

/**
 * 语音识别请求参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeechRecognitionRequest {
    
    /**
     * 模型名称，默认为fun-asr-realtime
     */
    @Builder.Default
    private String model = "fun-asr-realtime";
    
    /**
     * 音频格式：pcm、wav、mp3、opus、speex、aac、amr
     */
    @NotNull(message = "音频格式不能为空")
    @Pattern(regexp = "^(pcm|wav|mp3|opus|speex|aac|amr)$", message = "不支持的音频格式")
    private String format;
    
    /**
     * 采样率，支持16000Hz
     */
    @NotNull(message = "采样率不能为空")
    @Min(value = 8000, message = "采样率不能低于8000Hz")
    @Max(value = 48000, message = "采样率不能超过48000Hz")
    @Builder.Default
    private Integer sampleRate = 16000;
    
    /**
     * 热词ID
     */
    private String vocabularyId;
    
    /**
     * 热词ID(v1系列模型)
     */
    private String phraseId;
    
    /**
     * 是否开启语义断句
     */
    @Builder.Default
    private Boolean semanticPunctuationEnabled = false;
    
    /**
     * VAD断句静音时长阈值(ms)
     */
    @Min(value = 200, message = "静音阈值不能低于200ms")
    @Max(value = 6000, message = "静音阈值不能超过6000ms")
    @Builder.Default
    private Integer maxSentenceSilence = 1300;
    
    /**
     * 是否开启多阈值模式
     */
    @Builder.Default
    private Boolean multiThresholdModeEnabled = false;
    
    /**
     * 是否开启标点预测
     */
    @Builder.Default
    private Boolean punctuationPredictionEnabled = true;
    
    /**
     * 是否开启心跳保持长连接
     */
    @Builder.Default
    private Boolean heartbeat = false;
    
    /**
     * 语言提示(仅paraformer-realtime-v2模型支持)
     */
    private String[] languageHints;
    
    /**
     * 其他自定义参数
     */
    private Map<String, Object> additionalParameters;
    
    /**
     * 获取完整的参数Map
     */
    public Map<String, Object> toParameterMap() {
        Map<String, Object> params = new java.util.HashMap<>();
        
        if (semanticPunctuationEnabled != null) {
            params.put("semantic_punctuation_enabled", semanticPunctuationEnabled);
        }
        if (maxSentenceSilence != null) {
            params.put("max_sentence_silence", maxSentenceSilence);
        }
        if (multiThresholdModeEnabled != null) {
            params.put("multi_threshold_mode_enabled", multiThresholdModeEnabled);
        }
        if (punctuationPredictionEnabled != null) {
            params.put("punctuation_prediction_enabled", punctuationPredictionEnabled);
        }
        if (heartbeat != null) {
            params.put("heartbeat", heartbeat);
        }
        if (languageHints != null && languageHints.length > 0) {
            params.put("language_hints", languageHints);
        }
        
        // 添加自定义参数
        if (additionalParameters != null) {
            params.putAll(additionalParameters);
        }
        
        return params;
    }
}
