package com.carol.backend.service;

import com.carol.backend.dto.TtsSynthesisRequest;
import com.carol.backend.dto.TtsSynthesisResponse;
import com.carol.backend.entity.Character;
import com.carol.backend.config.TtsSynthesisConfig;
import com.carol.backend.util.TtsTextPreprocessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

/**
 * 聊天与TTS集成服务
 * 负责在聊天流程中自动触发语音合成
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatTtsIntegrationService {
    
    private final TtsSynthesisService ttsSynthesisService;
    private final CharacterService characterService;
    private final TtsSynthesisConfig ttsConfig;
    private final TtsTextPreprocessor textPreprocessor;
    
    /**
     * 为聊天回复自动生成语音
     * 
     * @param message 聊天回复内容
     * @param characterId 角色ID（可选）
     * @param userId 用户ID
     * @param languageType 语言类型（可选，默认中文）
     * @return TTS合成结果
     */
    public TtsSynthesisResponse generateSpeechForChatReply(String message, Long characterId, 
                                                          String userId, String languageType) {
        try {
            // 检查文本是否适合语音合成
            if (!textPreprocessor.isSuitableForTts(message, ttsConfig.getMaxTextLength())) {
                log.debug("文本不适合语音合成，跳过: userId={}, characterId={}", userId, characterId);
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
                log.info("聊天回复语音合成成功: userId={}, characterId={}, audioUrl={}", 
                        userId, characterId, response.getAudioUrl());
            } else {
                log.warn("聊天回复语音合成失败: userId={}, characterId={}, error={}", 
                        userId, characterId, response.getErrorMessage());
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("聊天回复语音合成异常: userId={}, characterId={}, error={}", 
                     userId, characterId, e.getMessage(), e);
            return null;
        }
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
                log.warn("获取角色音色配置失败，使用默认配置: characterId={}, error={}", 
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
            log.debug("选择角色模型时出错，使用默认模型: characterId={}, error={}", 
                     characterId, e.getMessage());
        }
        
        return defaultModel;
    }
    
    /**
     * 批量生成多个回复的语音
     */
    public void batchGenerateSpeechForReplies(java.util.List<String> messages, Long characterId, 
                                            String userId, String languageType) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        
        log.info("开始批量生成语音: count={}, characterId={}, userId={}", 
                messages.size(), characterId, userId);
        
        for (int i = 0; i < messages.size(); i++) {
            try {
                String message = messages.get(i);
                generateSpeechForChatReply(message, characterId, userId, languageType);
                
                // 添加短暂延迟避免过于频繁的API调用
                if (i < messages.size() - 1) {
                    Thread.sleep(100);
                }
                
            } catch (Exception e) {
                log.error("批量语音生成中出错: index={}, error={}", i, e.getMessage());
                // 继续处理下一条，不中断整个批次
            }
        }
        
        log.info("批量语音生成完成: characterId={}, userId={}", characterId, userId);
    }
}
