package com.carol.backend.service;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;

import com.carol.backend.config.TtsSynthesisConfig;
import com.carol.backend.dto.TtsSynthesisRequest;
import com.carol.backend.dto.TtsSynthesisResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.carol.backend.util.TtsSegmentUtil;
import com.carol.backend.util.TtsTextPreprocessor;


import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * TTS语音合成服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TtsSynthesisService {
    
    private final TtsSynthesisConfig config;
    private final TtsSegmentUtil ttsSegmentUtil;
    private final TtsTextPreprocessor textPreprocessor;
    
    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;
    
    /**
     * 同步语音合成
     */
    public TtsSynthesisResponse synthesizeText(TtsSynthesisRequest request) 
            throws ApiException, NoApiKeyException, UploadFileException {
        
        log.info("开始语音合成: 文本长度={}, 音色={}, 语言={}", 
                request.getText().length(), request.getVoice(), request.getLanguageType());
        
        LocalDateTime startTime = LocalDateTime.now();
        String requestId = UUID.randomUUID().toString();
        
        // 验证参数
        validateRequest(request);
        
        try {
            String audioUrl;
            byte[] audioData = null;
            
            // 检查文本长度，决定是否需要分段处理 // 580 字符上限
            if (request.getText().length() > 580) {
                log.info("文本超过580字符限制，启用分段TTS处理: {}字符", request.getText().length());
                
                // 分段处理
                List<String> segments = ttsSegmentUtil.splitText(request.getText());
                audioData = ttsSegmentUtil.synthesizeSegmentsConcurrently(
                    segments, 
                    request.getVoice(), 
                    request.getModel(), 
                    request.getLanguageType()
                );
                
                // 对于分段合成，我们需要上传合并后的音频
                audioUrl = "data:audio/wav;base64," + java.util.Base64.getEncoder().encodeToString(audioData);
                
            } else {
                // 原有的单次合成逻辑
                MultiModalConversationParam param = buildSynthesisParam(request);
                
                MultiModalConversation conv = new MultiModalConversation();
                MultiModalConversationResult result = conv.call(param);
                
                if (result == null || result.getOutput() == null || result.getOutput().getAudio() == null) {
                    throw new RuntimeException("语音合成返回结果为空");
                }
                
                audioUrl = result.getOutput().getAudio().getUrl();
            }
            
            LocalDateTime endTime = LocalDateTime.now();
            long processingTime = java.time.Duration.between(startTime, endTime).toMillis();
            
            // 构建响应
            TtsSynthesisResponse response = TtsSynthesisResponse.builder()
                    .success(true)
                    .audioUrl(audioUrl)
                    .format("wav")
                    .sampleRate(24000)
                    .voice(request.getVoice())
                    .languageType(request.getLanguageType())
                    .model(request.getModel())
                    .characterCount(request.getText().length())
                    .requestId(requestId)
                    .startTime(startTime)
                    .endTime(endTime)
                    .processingTime(processingTime)
                    .tokenUsage(calculateTokenUsage(request))
                    .estimatedCost(calculateCost(request))
                    .build();
            
            // 如果需要保存到本地
            if (Boolean.TRUE.equals(request.getSaveToLocal())) {
                String localPath = saveAudioToLocal(audioUrl, request.getFileName(), requestId);
                response.setLocalFilePath(localPath);
            }
            
            log.info("语音合成完成: requestId={}, 处理时间={}ms, audioUrl={}", 
                    requestId, processingTime, audioUrl);
            
            return response;
            
        } catch (Exception e) {
            log.error("语音合成失败: requestId={}, error={}", requestId, e.getMessage(), e);
            
            return TtsSynthesisResponse.builder()
                    .success(false)
                    .requestId(requestId)
                    .startTime(startTime)
                    .endTime(LocalDateTime.now())
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
    
    /**
     * 构建语音合成参数
     */
    private MultiModalConversationParam buildSynthesisParam(TtsSynthesisRequest request) {
        // 将字符串音色转换为Voice枚举
        com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters.Voice voiceEnum = 
            getVoiceEnum(request.getVoice());
        
        return MultiModalConversationParam.builder()
                .model(request.getModel())
                .apiKey(apiKey)
                .text(request.getText())
                .voice(voiceEnum)
                .languageType(request.getLanguageType())
                .build();
    }
    
    /**
     * 将字符串音色转换为Voice枚举
     */
    private com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters.Voice getVoiceEnum(String voiceName) {
        try {
            // 使用反射或直接映射
            switch (voiceName.toLowerCase()) {
                case "cherry": return com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters.Voice.CHERRY;
                case "ethan": return com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters.Voice.ETHAN;
                case "nofish": return com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters.Voice.NOFISH;
                case "jennifer": return com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters.Voice.JENNIFER;
                case "ryan": return com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters.Voice.RYAN;
                case "katerina": return com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters.Voice.KATERINA;
                case "elias": return com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters.Voice.ELIAS;
                case "jada": return com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters.Voice.JADA;
                case "dylan": return com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters.Voice.DYLAN;
                case "sunny": return com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters.Voice.SUNNY;
                case "marcus": return com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters.Voice.MARCUS;
                case "serena": return com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters.Voice.SERENA;
                case "chelsie": return com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters.Voice.CHELSIE;
                default: return com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters.Voice.CHERRY;
            }
        } catch (Exception e) {
            log.warn("无法识别音色: {}, 使用默认音色Cherry", voiceName);
            return com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters.Voice.CHERRY;
        }
    }
    
    /**
     * 验证请求参数
     */
    private void validateRequest(TtsSynthesisRequest request) {
        if (request.getText() == null || request.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("文本内容不能为空");
        }
        
        if (request.getText().length() > config.getMaxTextLength()) {
            throw new IllegalArgumentException("文本长度超过限制: " + config.getMaxTextLength());
        }
        
        // 检查音色语言支持
        if (!config.isVoiceLanguageSupported(request.getVoice(), request.getLanguageType())) {
            throw new IllegalArgumentException(
                String.format("音色 %s 不支持语言 %s", request.getVoice(), request.getLanguageType()));
        }
    }
    
    /**
     * 保存音频到本地
     */
    private String saveAudioToLocal(String audioUrl, String fileName, String requestId) {
        try {
            // 创建保存目录
            Path saveDir = Paths.get(config.getAudioSaveDirectory());
            if (!Files.exists(saveDir)) {
                Files.createDirectories(saveDir);
            }
            
            // 生成文件名
            String finalFileName = fileName != null ? fileName : "tts_" + requestId + ".wav";
            Path filePath = saveDir.resolve(finalFileName);
            
            // 下载音频文件
            try (InputStream in = new URL(audioUrl).openStream();
                 FileOutputStream out = new FileOutputStream(filePath.toFile())) {
                
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            
            log.info("音频文件已保存到本地: {}", filePath.toString());
            return filePath.toString();
            
        } catch (Exception e) {
            log.error("保存音频文件失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 计算Token使用量
     */
    private Integer calculateTokenUsage(TtsSynthesisRequest request) {
        if ("qwen3-tts-flash".equals(request.getModel())) {
            // Qwen3-TTS按字符计费
            return request.getText().length();
        } else {
            // Qwen-TTS按Token计费，估算为字符数的1.5倍
            return (int) (request.getText().length() * 1.5);
        }
    }
    
    /**
     * 计算费用
     */
    private Double calculateCost(TtsSynthesisRequest request) {
        if ("qwen3-tts-flash".equals(request.getModel())) {
            // Qwen3-TTS: 0.8元/万字符
            return (request.getText().length() / 10000.0) * config.getCost().getQwen3TtsPrice();
        } else {
            // Qwen-TTS: 按Token计费
            int tokens = calculateTokenUsage(request);
            double inputCost = (tokens / 1000.0) * config.getCost().getQwenTtsInputPrice();
            // 假设输出Token约为输入的20%
            double outputCost = (tokens * 0.2 / 1000.0) * config.getCost().getQwenTtsOutputPrice();
            return inputCost + outputCost;
        }
    }
    
    /**
     * 根据角色ID获取推荐音色
     */
    public String getRecommendedVoiceForCharacter(Long characterId) {
        return config.getCharacterVoice(characterId);
    }
    
    /**
     * 为角色合成语音（自动选择音色）
     */
    public TtsSynthesisResponse synthesizeForCharacter(String text, Long characterId, String languageType) 
            throws ApiException, NoApiKeyException, UploadFileException {
        
        log.info("角色语音合成开始: characterId={}, 原文本长度={}", characterId, text.length());
        
        // 检查文本是否适合语音合成
        if (!textPreprocessor.isSuitableForTts(text, config.getMaxTextLength())) {
            log.warn("文本不适合语音合成，跳过: characterId={}, 文本长度={}", characterId, text.length());
            return TtsSynthesisResponse.builder()
                    .success(false)
                    .errorMessage("文本不适合语音合成")
                    .build();
        }
        
        // 预处理文本（清理Markdown格式、特殊字符等）
        String processedText = textPreprocessor.preprocessTextForTts(text);
        
        // 确定最终的语言类型
        String finalLanguageType = textPreprocessor.determineLanguageType(processedText, languageType);
        
        // 获取推荐音色
        String recommendedVoice = getRecommendedVoiceForCharacter(characterId);
        
        log.info("角色语音合成参数: characterId={}, 处理后文本长度={}, 语言={}, 音色={}", 
                characterId, processedText.length(), finalLanguageType, recommendedVoice);
        
        TtsSynthesisRequest request = TtsSynthesisRequest.builder()
                .text(processedText)  // 使用预处理后的文本
                .voice(recommendedVoice)
                .languageType(finalLanguageType)
                .model(config.getDefaultModel())
                .characterId(characterId)
                .build();
                
        return synthesizeText(request);
    }
}
