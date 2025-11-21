package com.carol.backend.service.impl;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.carol.backend.config.TtsSynthesisConfig;
import com.carol.backend.dto.TtsSynthesisRequest;
import com.carol.backend.dto.StreamingTtsResponse;
import com.carol.backend.enums.ErrorCode;
import com.carol.backend.exception.BusinessException;
import com.carol.backend.service.IStreamingTtsSynthesisService;
import com.carol.backend.util.TtsSegmentUtil;
import io.reactivex.Flowable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 流式TTS语音合成服务实现类
 * 
 * @author jianjl
 * @version 1.0
 * @description 流式TTS语音合成服务实现
 * @date 2025-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingTtsSynthesisServiceImpl implements IStreamingTtsSynthesisService {
    
    private final TtsSynthesisConfig config;
    private final TtsSegmentUtil ttsSegmentUtil;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    // 存储活动的流式会话
    private final ConcurrentHashMap<String, SseEmitter> activeSessions = new ConcurrentHashMap<>();
    
    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;
    
    @Override
    public SseEmitter createStreamingSynthesis(TtsSynthesisRequest request) {
        String sessionId = UUID.randomUUID().toString();
        log.info("[createStreamingSynthesis] 创建流式语音合成会话: sessionId={}, textLength={}", 
                sessionId, request.getText().length());
        
        // 设置为流式模式
        request.setStream(true);
        
        SseEmitter emitter = new SseEmitter(config.getStreamingTimeout() * 1000L);
        activeSessions.put(sessionId, emitter);
        
        // 设置emitter回调
        emitter.onCompletion(() -> {
            log.info("[createStreamingSynthesis] 流式TTS会话完成: sessionId={}", sessionId);
            activeSessions.remove(sessionId);
        });
        
        emitter.onTimeout(() -> {
            log.warn("[createStreamingSynthesis] 流式TTS会话超时: sessionId={}", sessionId);
            activeSessions.remove(sessionId);
        });
        
        emitter.onError((throwable) -> {
            log.error("[createStreamingSynthesis] 流式TTS会话错误: sessionId={}, error={}", 
                    sessionId, throwable.getMessage());
            activeSessions.remove(sessionId);
        });
        
        // 异步执行流式合成
        executorService.submit(() -> performStreamingSynthesis(request, emitter, sessionId));
        
        return emitter;
    }
    
    @Override
    public boolean stopStreamingSynthesis(String sessionId) {
        log.info("[stopStreamingSynthesis] 停止流式合成会话: sessionId={}", sessionId);
        
        SseEmitter emitter = activeSessions.remove(sessionId);
        if (emitter != null) {
            try {
                StreamingTtsResponse stopResponse = StreamingTtsResponse.builder()
                        .type("stop")
                        .isFinished(true)
                        .sessionId(sessionId)
                        .timestamp(LocalDateTime.now())
                        .status("stopped")
                        .build();
                        
                sendSseEvent(emitter, "synthesis-stopped", stopResponse);
                emitter.complete();
                
                log.info("[stopStreamingSynthesis] 流式TTS会话已停止: sessionId={}", sessionId);
                return true;
            } catch (Exception e) {
                log.error("[stopStreamingSynthesis] 停止流式TTS会话失败: sessionId={}, error={}", 
                        sessionId, e.getMessage(), e);
                emitter.completeWithError(e);
                return false;
            }
        }
        
        log.warn("[stopStreamingSynthesis] 会话不存在: sessionId={}", sessionId);
        return false;
    }
    
    @Override
    public int getActiveSessionCount() {
        int count = activeSessions.size();
        log.debug("[getActiveSessionCount] 当前活动会话数: {}", count);
        return count;
    }
    
    @Override
    public void cleanupAllSessions() {
        log.info("[cleanupAllSessions] 清理所有流式TTS会话，当前活动数量: {}", activeSessions.size());
        
        activeSessions.forEach((sessionId, emitter) -> {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.warn("[cleanupAllSessions] 清理会话时出错: sessionId={}, error={}", 
                        sessionId, e.getMessage(), e);
            }
        });
        
        activeSessions.clear();
        log.info("[cleanupAllSessions] 所有会话已清理");
    }
    
    /**
     * 执行流式语音合成
     */
    private void performStreamingSynthesis(TtsSynthesisRequest request, SseEmitter emitter, String sessionId) {
        try {
            log.info("[performStreamingSynthesis] 开始流式语音合成: sessionId={}, textLength={}, voice={}", 
                    sessionId, request.getText().length(), request.getVoice());
            
            // 验证参数
            validateRequest(request);
            
            // 检查文本长度，决定是否需要分段处理 // 580 字符上限
            if (request.getText().length() > 580) {
                log.info("[performStreamingSynthesis] 流式TTS文本超过580字符限制，启用分段处理: {}字符", 
                        request.getText().length());
                performSegmentedStreamingSynthesis(request, emitter, sessionId);
                return;
            }
            
            // 原有的单次流式合成逻辑
            MultiModalConversationParam param = buildStreamingSynthesisParam(request);
            
            // 执行流式语音合成
            MultiModalConversation conv = new MultiModalConversation();
            Flowable<MultiModalConversationResult> resultStream = conv.streamCall(param);
            
            // 处理流式数据
            AtomicInteger sequenceNumber = new AtomicInteger(1);
            AtomicReference<Double> totalDuration = new AtomicReference<>(0.0);
            AtomicReference<String> completeAudioUrl = new AtomicReference<>();
            
            resultStream.blockingForEach(result -> {
                try {
                    if (result.getOutput() != null && result.getOutput().getAudio() != null) {
                        String audioData = result.getOutput().getAudio().getData();
                        String audioUrl = result.getOutput().getAudio().getUrl();
                        
                        if (audioData != null) {
                            // 计算当前数据块时长（估算）
                            double chunkDuration = estimateChunkDuration(audioData);
                            totalDuration.updateAndGet(current -> current + chunkDuration);
                            
                            // 发送音频数据块
                            StreamingTtsResponse response = StreamingTtsResponse.createChunk(
                                    audioData, 
                                    sequenceNumber.getAndIncrement(),
                                    chunkDuration,
                                    totalDuration.get(),
                                    sessionId
                            );
                            response.setVoice(request.getVoice());
                            response.setLanguageType(request.getLanguageType());
                            
                            sendSseEvent(emitter, "audio-chunk", response);
                        }
                        
                        // 保存完整音频URL
                        if (audioUrl != null) {
                            completeAudioUrl.set(audioUrl);
                        }
                        
                        // 检查是否完成
                        if ("stop".equals(result.getOutput().getFinishReason())) {
                            // 发送最终响应
                            StreamingTtsResponse finalResponse = StreamingTtsResponse.createFinal(
                                    completeAudioUrl.get(),
                                    totalDuration.get(),
                                    sessionId
                            );
                            sendSseEvent(emitter, "synthesis-complete", finalResponse);
                            
                            log.info("[performStreamingSynthesis] 流式语音合成完成: sessionId={}, totalDuration={}s", 
                                    sessionId, totalDuration.get());
                            emitter.complete();
                        }
                    }
                } catch (Exception e) {
                    log.error("[performStreamingSynthesis] 处理流式TTS数据时出错: sessionId={}, error={}", 
                            sessionId, e.getMessage(), e);
                    throw e;
                }
            });
            
        } catch (Exception e) {
            log.error("[performStreamingSynthesis] 流式语音合成失败: sessionId={}, error={}", 
                    sessionId, e.getMessage(), e);
            
            try {
                StreamingTtsResponse errorResponse = StreamingTtsResponse.createError(
                        e.getMessage(),
                        "SYNTHESIS_ERROR",
                        sessionId
                );
                sendSseEvent(emitter, "error", errorResponse);
                emitter.complete();
            } catch (Exception sendError) {
                log.error("[performStreamingSynthesis] 发送错误响应失败: sessionId={}, error={}", 
                        sessionId, sendError.getMessage(), e);
                emitter.completeWithError(sendError);
            }
        }
    }
    
    /**
     * 构建流式合成参数
     */
    private MultiModalConversationParam buildStreamingSynthesisParam(TtsSynthesisRequest request) {
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
                default: 
                    log.warn("[getVoiceEnum] 无法识别音色: {}, 使用默认音色Cherry", voiceName);
                    return com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters.Voice.CHERRY;
            }
        } catch (Exception e) {
            log.warn("[getVoiceEnum] 无法识别音色: {}, 使用默认音色Cherry, error={}", voiceName, e.getMessage());
            return com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters.Voice.CHERRY;
        }
    }
    
    /**
     * 估算音频数据块时长
     */
    private double estimateChunkDuration(String base64AudioData) {
        if (base64AudioData == null || base64AudioData.isEmpty()) {
            return 0.0;
        }
        
        // Base64解码后的字节数
        int decodedBytes = (base64AudioData.length() * 3) / 4;
        
        // PCM 16-bit, 24kHz, 单声道
        // 每秒字节数 = 采样率 * (位深度/8) * 声道数 = 24000 * 2 * 1 = 48000 bytes/sec
        double duration = (double) decodedBytes / 48000.0;
        
        return Math.max(duration, 0.0);
    }
    
    /**
     * 发送SSE事件
     */
    private void sendSseEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data)
                    .id(UUID.randomUUID().toString()));
        } catch (Exception e) {
            log.error("[sendSseEvent] 发送SSE事件失败: event={}, error={}", eventName, e.getMessage(), e);
            throw new RuntimeException("发送SSE事件失败", e);
        }
    }
    
    /**
     * 验证请求参数
     */
    private void validateRequest(TtsSynthesisRequest request) {
        if (request.getText() == null || request.getText().trim().isEmpty()) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "文本内容不能为空");
        }
        
        if (request.getText().length() > config.getMaxTextLength()) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, 
                "文本长度超过限制: " + config.getMaxTextLength());
        }
        
        // 检查音色语言支持
        if (!config.isVoiceLanguageSupported(request.getVoice(), request.getLanguageType())) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR,
                String.format("音色 %s 不支持语言 %s", request.getVoice(), request.getLanguageType()));
        }
    }
    
    /**
     * 执行分段流式语音合成 // 580 字符上限
     */
    private void performSegmentedStreamingSynthesis(TtsSynthesisRequest request, SseEmitter emitter, String sessionId) {
        log.info("[performSegmentedStreamingSynthesis] 开始分段流式语音合成: sessionId={}, textLength={}", 
                sessionId, request.getText().length());
        
        try {
            // 分段处理
            List<String> segments = ttsSegmentUtil.splitText(request.getText());
            log.info("[performSegmentedStreamingSynthesis] 分段流式TTS: sessionId={}, segmentCount={}", 
                    sessionId, segments.size());
            
            AtomicInteger globalSequenceNumber = new AtomicInteger(1);
            AtomicReference<Double> totalDuration = new AtomicReference<>(0.0);
            
            // 依次处理每个段落
            for (int i = 0; i < segments.size(); i++) {
                String segment = segments.get(i);
                boolean isLastSegment = (i == segments.size() - 1);
                
                log.debug("[performSegmentedStreamingSynthesis] 处理段落 {}/{}: segmentLength={}", 
                        i+1, segments.size(), segment.length());
                
                // 为每个段落创建独立的请求
                TtsSynthesisRequest segmentRequest = TtsSynthesisRequest.builder()
                        .text(segment)
                        .voice(request.getVoice())
                        .model(request.getModel())
                        .languageType(request.getLanguageType())
                        .stream(true)
                        .build();
                
                // 构建段落合成参数
                MultiModalConversationParam param = buildStreamingSynthesisParam(segmentRequest);
                
                // 执行段落流式合成
                MultiModalConversation conv = new MultiModalConversation();
                Flowable<MultiModalConversationResult> resultStream = conv.streamCall(param);
                
                // 处理段落流式数据
                resultStream.blockingForEach(result -> {
                    try {
                        if (result.getOutput() != null && result.getOutput().getAudio() != null) {
                            String audioData = result.getOutput().getAudio().getData();
                            String audioUrl = result.getOutput().getAudio().getUrl();
                            
                            if (audioData != null) {
                                // 计算当前数据块时长
                                double chunkDuration = estimateChunkDuration(audioData);
                                totalDuration.updateAndGet(current -> current + chunkDuration);
                                
                                // 发送音频数据块
                                StreamingTtsResponse response = StreamingTtsResponse.createChunk(
                                        audioData, 
                                        globalSequenceNumber.getAndIncrement(),
                                        chunkDuration,
                                        totalDuration.get(),
                                        sessionId
                                );
                                
                                emitter.send(SseEmitter.event()
                                        .name("audio_chunk")
                                        .data(response));
                                
                                log.debug("[performSegmentedStreamingSynthesis] 发送段落音频块: sequenceNumber={}, chunkDuration={}s", 
                                        response.getSequenceNumber(), chunkDuration);
                            }
                            
                            // 如果有完整音频URL（段落结束）
                            if (audioUrl != null && isLastSegment) {
                                StreamingTtsResponse finalResponse = StreamingTtsResponse.createFinal(
                                        audioUrl,
                                        totalDuration.get(),
                                        sessionId
                                );
                                
                                emitter.send(SseEmitter.event()
                                        .name("synthesis_complete")
                                        .data(finalResponse));
                                
                                log.info("[performSegmentedStreamingSynthesis] 分段流式TTS完成: sessionId={}, totalDuration={}s", 
                                        sessionId, totalDuration.get());
                            }
                        }
                    } catch (Exception e) {
                        log.error("[performSegmentedStreamingSynthesis] 发送段落流式数据失败: sessionId={}, error={}", 
                                sessionId, e.getMessage(), e);
                        throw new RuntimeException("发送流式数据失败", e);
                    }
                });
                
                // 段落间添加微小停顿（通过发送静音块）
                if (!isLastSegment) {
                    Thread.sleep(300); // 0.3秒间隔
                }
            }
            
            // 完成流式传输
            emitter.complete();
            
        } catch (Exception e) {
            log.error("[performSegmentedStreamingSynthesis] 分段流式语音合成失败: sessionId={}, error={}", 
                    sessionId, e.getMessage(), e);
            
            try {
                StreamingTtsResponse errorResponse = StreamingTtsResponse.createError(
                        "分段流式合成失败: " + e.getMessage(),
                        "SEGMENT_TTS_ERROR",
                        sessionId
                );
                emitter.send(SseEmitter.event().name("synthesis_error").data(errorResponse));
                emitter.completeWithError(e);
            } catch (Exception sendError) {
                log.error("[performSegmentedStreamingSynthesis] 发送错误响应失败: error={}", sendError.getMessage());
                emitter.completeWithError(e);
            }
        }
    }
}
