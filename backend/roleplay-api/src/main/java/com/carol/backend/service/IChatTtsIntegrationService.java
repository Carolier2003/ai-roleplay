package com.carol.backend.service;

import com.carol.backend.dto.TtsSynthesisResponse;

import java.util.List;

/**
 * 聊天与TTS集成服务接口
 * 
 * @author jianjl
 * @version 1.0
 * @description 负责在聊天流程中自动触发语音合成
 * @date 2025-01-15
 */
public interface IChatTtsIntegrationService {
    
    /**
     * 为聊天回复自动生成语音
     * 
     * @param message 聊天回复内容
     * @param characterId 角色ID（可选）
     * @param userId 用户ID
     * @param languageType 语言类型（可选，默认中文）
     * @return TTS合成结果
     */
    TtsSynthesisResponse generateSpeechForChatReply(String message, Long characterId, 
                                                   String userId, String languageType);
    
    /**
     * 批量生成多个回复的语音
     * 
     * @param messages 消息列表
     * @param characterId 角色ID
     * @param userId 用户ID
     * @param languageType 语言类型
     */
    void batchGenerateSpeechForReplies(List<String> messages, Long characterId, 
                                      String userId, String languageType);
}
