package com.carol.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TTS音频持久化结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TtsPersistenceResult {
    /**
     * OSS音频URL
     */
    private String audioUrl;

    /**
     * 音频时长（秒）
     */
    private Integer duration;
}
