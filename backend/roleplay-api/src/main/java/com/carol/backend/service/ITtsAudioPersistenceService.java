package com.carol.backend.service;

import com.carol.backend.dto.TtsPersistenceResult;

/**
 * TTS音频持久化服务接口
 * 
 * @author jianjl
 * @version 1.0
 * @description 负责将阿里云TTS返回的临时URL音频下载并上传到OSS获取永久URL
 * @date 2025-01-22
 */
public interface ITtsAudioPersistenceService {
    
    /**
     * 持久化TTS音频
     * @param temporaryUrl 临时URL
     * @param userId 用户ID
     * @param characterId 角色ID
     * @return 持久化结果（包含URL和时长）
     */
    TtsPersistenceResult persistTtsAudio(String temporaryUrl, Long userId, Long characterId);
    
    /**
     * 从临时URL下载音频并上传到OSS（简化版本）
     * 
     * @param temporaryUrl 阿里云TTS返回的临时URL
     * @param userId 用户ID
     * @return OSS永久URL
     * @throws RuntimeException 如果下载或上传失败
     */
    default TtsPersistenceResult persistTtsAudio(String temporaryUrl, Long userId) {
        return persistTtsAudio(temporaryUrl, userId, null);
    }
}
