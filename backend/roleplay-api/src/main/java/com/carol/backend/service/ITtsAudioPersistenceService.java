package com.carol.backend.service;

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
     * 从临时URL下载音频并上传到OSS
     * 
     * @param temporaryUrl 阿里云TTS返回的临时URL
     * @param userId 用户ID
     * @param characterId 角色ID（可选）
     * @return OSS永久URL
     * @throws RuntimeException 如果下载或上传失败
     */
    String persistTtsAudio(String temporaryUrl, Long userId, Long characterId);
    
    /**
     * 从临时URL下载音频并上传到OSS（简化版本）
     * 
     * @param temporaryUrl 阿里云TTS返回的临时URL
     * @param userId 用户ID
     * @return OSS永久URL
     * @throws RuntimeException 如果下载或上传失败
     */
    default String persistTtsAudio(String temporaryUrl, Long userId) {
        return persistTtsAudio(temporaryUrl, userId, null);
    }
}
