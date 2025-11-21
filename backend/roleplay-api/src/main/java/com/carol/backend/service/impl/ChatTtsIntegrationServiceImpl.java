package com.carol.backend.service.impl;

import com.carol.backend.config.TtsSynthesisConfig;
import com.carol.backend.dto.TtsSynthesisRequest;
import com.carol.backend.dto.TtsSynthesisResponse;
import com.carol.backend.entity.Character;
import com.carol.backend.enums.ErrorCode;
import com.carol.backend.exception.BusinessException;
import com.carol.backend.service.IChatTtsIntegrationService;
import com.carol.backend.service.CharacterService;
import com.carol.backend.service.ITtsSynthesisService;
import com.carol.backend.util.TtsTextPreprocessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 聊天与TTS集成服务实现类
 * 
 * @author jianjl
 * @version 1.0
 * @description 负责在聊天流程中自动触发语音合成
 * @date 2025-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatTtsIntegrationServiceImpl implements IChatTtsIntegrationService {
    
    private final ITtsSynthesisService ttsSynthesisService;
    private final CharacterService characterService;
    private final TtsSynthesisConfig ttsConfig;
    private final TtsTextPreprocessor textPreprocessor;
    
    @Override
    public TtsSynthesisResponse generateSpeechForChatReply(String message, Long characterId, 
                                                          String userId, String languageType) {
        log.info("[generateSpeechForChatReply] 开始生成语音: userId={}, characterId={}, messageLength={}", 
                userId, characterId, message != null ? message.length() : 0);
        
        try {
            // 检查文本是否适合语音合成
            if (!textPreprocessor.isSuitableForTts(message, ttsConfig.getMaxTextLength())) {
                log.debug("[generateSpeechForChatReply] 文本不适合语音合成，跳过: userId={}, characterId={}", 
                        userId, characterId);
                return null;
            }
            
            // 预处理文本（清理特殊字符、格式化等）
            String processedText = textPreprocessor.preprocessTextForTts(message);
            
            // 确定语言类型
            String finalLanguageType = textPreprocessor.determineLanguageType(processedText, languageType);
            
            // 构建TTS请求
            TtsSynthesisRequest ttsRequest = buildTtsRequest(
                processedText, characterId, userId, finalLanguageType);
            
            // 执行语音合成
            TtsSynthesisResponse response = ttsSynthesisService.synthesizeText(ttsRequest);
            
            if (response.getSuccess()) {
                log.info("[generateSpeechForChatReply] 聊天回复语音合成成功: userId={}, characterId={}, audioUrl={}", 
                        userId, characterId, response.getAudioUrl());
            } else {
                log.warn("[generateSpeechForChatReply] 聊天回复语音合成失败: userId={}, characterId={}, error={}", 
                        userId, characterId, response.getErrorMessage());
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("[generateSpeechForChatReply] 聊天回复语音合成异常: userId={}, characterId={}, error={}", 
                     userId, characterId, e.getMessage(), e);
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "语音合成失败", e);
        }
    }
    
    @Override
    public void batchGenerateSpeechForReplies(List<String> messages, Long characterId, 
                                            String userId, String languageType) {
        log.info("[batchGenerateSpeechForReplies] 开始批量生成语音: count={}, characterId={}, userId={}", 
                messages.size(), characterId, userId);
        
        if (messages == null || messages.isEmpty()) {
            log.warn("[batchGenerateSpeechForReplies] 消息列表为空，跳过");
            return;
        }
        
        int successCount = 0;
        int failCount = 0;
        
        for (int i = 0; i < messages.size(); i++) {
            try {
                String message = messages.get(i);
                TtsSynthesisResponse response = generateSpeechForChatReply(message, characterId, userId, languageType);
                
                if (response != null && response.getSuccess()) {
                    successCount++;
                } else {
                    failCount++;
                }
                
                // 添加短暂延迟避免过于频繁的API调用
                if (i < messages.size() - 1) {
                    Thread.sleep(100);
                }
                
            } catch (Exception e) {
                log.error("[batchGenerateSpeechForReplies] 批量语音生成中出错: index={}, error={}", 
                        i, e.getMessage(), e);
                failCount++;
                // 继续处理下一条，不中断整个批次
            }
        }
        
        log.info("[batchGenerateSpeechForReplies] 批量语音生成完成: characterId={}, userId={}, success={}, fail={}", 
                characterId, userId, successCount, failCount);
    }
    
    /**
     * 构建TTS请求
     */
    private TtsSynthesisRequest buildTtsRequest(String text, Long characterId, 
                                               String userId, String languageType) {
        // 获取角色信息和推荐音色
        String voice = ttsConfig.getDefaultVoice();
        String model = ttsConfig.getDefaultModel();
        
        if (characterId != null) {
            try {
                // 获取角色推荐音色
                voice = ttsSynthesisService.getRecommendedVoiceForCharacter(characterId);
                
                // 根据角色选择最佳模型
                model = selectOptimalModelForCharacter(characterId, languageType);
                
            } catch (Exception e) {
                log.warn("[buildTtsRequest] 获取角色音色配置失败，使用默认配置: characterId={}, error={}", 
                        characterId, e.getMessage());
            }
        }
        
        return TtsSynthesisRequest.builder()
                .text(text)
                .voice(voice)
                .languageType(languageType)
                .model(model)
                .characterId(characterId)
                .userId(userId)
                .stream(false)
                .saveToLocal(false)
                .build();
    }
    
    /**
     * 为角色选择最佳模型
     */
    private String selectOptimalModelForCharacter(Long characterId, String languageType) {
        // 默认使用qwen3-tts-flash（支持更多音色和语言）
        String defaultModel = "qwen3-tts-flash";
        
        try {
            Character character = characterService.getCharacterById(characterId);
            if (character != null) {
                // 根据角色特性选择模型
                String characterName = character.getName().toLowerCase();
                
                // 对于某些特定角色，可能需要特定模型
                if (characterName.contains("哈利") || characterName.contains("harry")) {
                    return "qwen3-tts-flash";  // 多语言支持，适合魔法世界
                } else if (characterName.contains("苏格拉底") || characterName.contains("socrates")) {
                    return "qwen3-tts-flash";  // 哲学家需要庄重的音色
                } else if (characterName.contains("爱因斯坦") || characterName.contains("einstein")) {
                    return "qwen3-tts-flash";  // 科学家
                }
            }
        } catch (Exception e) {
            log.debug("[selectOptimalModelForCharacter] 选择角色模型时出错，使用默认模型: characterId={}, error={}", 
                     characterId, e.getMessage());
        }
        
        return defaultModel;
    }
}
