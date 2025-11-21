package com.carol.backend.service.impl;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.carol.backend.config.SpeechRecognitionConfig;
import com.carol.backend.dto.SpeechRecognitionRequest;
import com.carol.backend.dto.SpeechRecognitionResponse;
import com.carol.backend.dto.StreamingSpeechRecognitionResponse;
import com.carol.backend.enums.ErrorCode;
import com.carol.backend.exception.BusinessException;
import com.carol.backend.service.IStreamingSpeechRecognitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 流式语音识别服务实现类
 * 
 * @author jianjl
 * @version 1.0
 * @description 流式语音识别服务实现
 * @date 2025-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingSpeechRecognitionServiceImpl implements IStreamingSpeechRecognitionService {
    
    private final SpeechRecognitionConfig config;
    
    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;
    
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    // 管理活跃的识别会话
    private final Map<String, RecognitionSession> activeSessions = new ConcurrentHashMap<>();
    
    /**
     * 初始化时设置全局API密钥
     */
    @PostConstruct
    private void initializeApiKey() {
        System.setProperty("dashscope.api-key", apiKey);
        log.debug("[initializeApiKey] 流式ASR服务已设置DashScope API密钥系统属性");
    }
    
    @Override
    public String createStreamingSession(SpeechRecognitionRequest request, SseEmitter emitter) 
            throws NoApiKeyException {
        
        log.info("[createStreamingSession] 创建流式语音识别会话: languageHints={}, format={}", 
                request.getLanguageHints() != null ? String.join(",", request.getLanguageHints()) : "null", 
                request.getFormat());
        
        // 创建识别器
        Recognition recognizer = new Recognition();
        
        // 构建参数
        RecognitionParam param = buildRecognitionParam(request);
        
        // 创建会话
        RecognitionSession session = new RecognitionSession(recognizer, emitter, param);
        String sessionId = session.getSessionId();
        
        activeSessions.put(sessionId, session);
        
        // 设置SSE超时时间和完成回调 (超时时间在创建时已设置)
        emitter.onCompletion(() -> {
            log.info("[createStreamingSession] SSE连接完成: sessionId={}", sessionId);
            closeSession(sessionId);
        });
        emitter.onTimeout(() -> {
            log.warn("[createStreamingSession] SSE连接超时: sessionId={}", sessionId);
            closeSession(sessionId);
        });
        emitter.onError(throwable -> {
            log.error("[createStreamingSession] SSE连接错误: sessionId={}, error={}", sessionId, throwable.getMessage(), throwable);
            closeSession(sessionId);
        });
        
        // 创建回调处理器
        ResultCallback<RecognitionResult> callback = new ResultCallback<RecognitionResult>() {
            @Override
            public void onEvent(RecognitionResult result) {
                try {
                    // 构建响应
                    SpeechRecognitionResponse response = convertRecognitionResult(result, recognizer);
                    
                    // 发送SSE事件
                    StreamingSpeechRecognitionResponse sseResponse = 
                        StreamingSpeechRecognitionResponse.result(sessionId, null);
                    sseResponse.setResult(createWordTimestamp(response));
                    
                    emitter.send(SseEmitter.event()
                        .name("recognition")
                        .data(sseResponse));
                    
                    log.debug("[createStreamingSession] 发送识别结果: sessionId={}, textLength={}", 
                            sessionId, response.getText() != null ? response.getText().length() : 0);
                    
                } catch (IOException e) {
                    log.error("[createStreamingSession] 发送SSE事件失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
                    closeSession(sessionId);
                }
            }
            
            @Override
            public void onComplete() {
                try {
                    log.info("[createStreamingSession] 流式识别完成: sessionId={}", sessionId);
                    
                    // 标记识别已完成，避免重复停止
                    session.setRecognitionStarted(false);
                    
                    StreamingSpeechRecognitionResponse completeResponse = 
                        StreamingSpeechRecognitionResponse.complete(sessionId);
                    
                    emitter.send(SseEmitter.event()
                        .name("complete")
                        .data(completeResponse));
                        
                    emitter.complete();
                    
                } catch (IOException e) {
                    log.error("[createStreamingSession] 发送完成事件失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
                } finally {
                    closeSession(sessionId);
                }
            }
            
            @Override
            public void onError(Exception e) {
                try {
                    log.error("[createStreamingSession] 流式识别错误: sessionId={}, error={}", sessionId, e.getMessage(), e);
                    
                    StreamingSpeechRecognitionResponse errorResponse = 
                        StreamingSpeechRecognitionResponse.error(sessionId, e.getMessage());
                    
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data(errorResponse));
                        
                    emitter.completeWithError(e);
                    
                } catch (IOException ioException) {
                    log.error("[createStreamingSession] 发送错误事件失败: sessionId={}, error={}", sessionId, ioException.getMessage(), ioException);
                } finally {
                    closeSession(sessionId);
                }
            }
        };
        
        // 异步启动识别
        executorService.submit(() -> {
            try {
                // 发送连接建立事件
                StreamingSpeechRecognitionResponse connectedResponse = 
                    StreamingSpeechRecognitionResponse.connected(sessionId);
                emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(connectedResponse));
                
                // 启动流式识别
                recognizer.call(param, callback);
                session.setRecognitionStarted(true);
                
                log.info("流式识别已启动，会话ID: {}", sessionId);
                
            } catch (Exception e) {
                log.error("启动流式识别失败，会话ID: {}", sessionId, e);
                callback.onError(e);
            }
        });
        
        return sessionId;
    }
    
    /**
     * 发送音频数据
     */
    @Override
    public void sendAudioData(String sessionId, byte[] audioData) {
        log.debug("[sendAudioData] 发送音频数据: sessionId={}, audioDataLength={}", sessionId, audioData.length);
        RecognitionSession session = activeSessions.get(sessionId);
        if (session == null) {
            log.warn("[sendAudioData] 会话不存在: sessionId={}", sessionId);
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "会话不存在: " + sessionId);
        }
        
        if (!session.isRecognitionStarted()) {
            log.warn("[sendAudioData] 识别尚未启动: sessionId={}", sessionId);
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "识别尚未启动");
        }
        
        try {
            ByteBuffer buffer = ByteBuffer.wrap(audioData);
            session.getRecognizer().sendAudioFrame(buffer);
            
            log.debug("[sendAudioData] 发送音频数据成功: sessionId={}, audioDataLength={}", sessionId, audioData.length);
            
        } catch (Exception e) {
            log.error("[sendAudioData] 发送音频数据失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "发送音频数据失败", e);
        }
    }
    
    /**
     * 停止识别
     */
    @Override
    public void stopRecognition(String sessionId) {
        log.info("[stopRecognition] 停止识别: sessionId={}", sessionId);
        RecognitionSession session = activeSessions.get(sessionId);
        if (session != null && session.isRecognitionStarted()) {
            try {
                session.getRecognizer().stop();
                log.info("[stopRecognition] 识别已停止: sessionId={}", sessionId);
            } catch (Exception e) {
                log.error("[stopRecognition] 停止识别失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            }
        } else {
            log.warn("[stopRecognition] 会话不存在或识别未启动: sessionId={}", sessionId);
        }
    }
    
    /**
     * 关闭会话
     */
    @Override
    public void closeSession(String sessionId) {
        log.info("[closeSession] 关闭会话: sessionId={}", sessionId);
        RecognitionSession session = activeSessions.remove(sessionId);
        if (session != null) {
            try {
                // 停止识别 - 只有在识别已启动且未完成的情况下才调用stop
                if (session.isRecognitionStarted()) {
                    try {
                        session.getRecognizer().stop();
                        log.debug("[closeSession] 识别器已停止: sessionId={}", sessionId);
                    } catch (Exception stopException) {
                        // 如果识别器已经自动停止，忽略状态错误
                        if (stopException.getMessage() != null && 
                            stopException.getMessage().contains("State invalid")) {
                            log.debug("[closeSession] 识别器已自动停止，跳过手动停止: sessionId={}", sessionId);
                        } else {
                            log.warn("[closeSession] 停止识别器时出错: sessionId={}, error={}", 
                                    sessionId, stopException.getMessage(), stopException);
                        }
                    }
                }
                
                // 关闭WebSocket连接
                try {
                    session.getRecognizer().getDuplexApi().close(1000, "会话结束");
                    log.debug("[closeSession] WebSocket连接已关闭: sessionId={}", sessionId);
                } catch (Exception closeException) {
                    // WebSocket可能已经关闭，记录但不抛出异常
                    log.debug("[closeSession] WebSocket连接关闭时出现异常（可能已关闭）: sessionId={}, error={}", 
                             sessionId, closeException.getMessage());
                }
                
                log.info("[closeSession] 会话已关闭: sessionId={}", sessionId);
                
            } catch (Exception e) {
                log.error("[closeSession] 关闭会话失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            }
        } else {
            log.debug("[closeSession] 会话不存在或已关闭: sessionId={}", sessionId);
        }
    }
    
    /**
     * 获取活跃会话数量
     */
    @Override
    public int getActiveSessionCount() {
        int count = activeSessions.size();
        log.debug("[getActiveSessionCount] 获取活跃会话数量: count={}", count);
        return count;
    }
    
    /**
     * 构建识别参数
     */
    private RecognitionParam buildRecognitionParam(SpeechRecognitionRequest request) throws NoApiKeyException {
        // 使用正确的Builder模式
        RecognitionParam.RecognitionParamBuilder<?, ?> builder = RecognitionParam.builder()
                .apiKey(apiKey)
                .model(request.getModel() != null ? request.getModel() : config.getDefaultModel())
                .format(request.getFormat() != null ? request.getFormat() : config.getDefaultFormat())
                .sampleRate(request.getSampleRate() != null ? request.getSampleRate() : config.getDefaultSampleRate());
        
        // 设置热词ID
        if (request.getVocabularyId() != null) {
            builder.vocabularyId(request.getVocabularyId());
        }
        if (request.getPhraseId() != null) {
            builder.phraseId(request.getPhraseId());
        }
        
        // 设置其他参数
        Map<String, Object> parameters = request.toParameterMap();
        if (!parameters.isEmpty()) {
            builder.parameters(parameters);
        }
        
        return builder.build();
    }
    
    /**
     * 转换识别结果
     */
    private SpeechRecognitionResponse convertRecognitionResult(RecognitionResult result, Recognition recognizer) {
        return SpeechRecognitionResponse.builder()
                .requestId(result.getRequestId())
                .text(result.getSentence().getText())
                .confidence(0.95) // 默认置信度，将来可从DashScope结果中提取
                .isSentenceEnd(result.isSentenceEnd())
                .beginTime(result.getSentence().getBeginTime())
                .endTime(result.getSentence().getEndTime())
                .words(convertWords(null)) // 暂时传null，等SDK API确认后实现
                .firstPackageDelay(recognizer.getFirstPackageDelay())
                .lastPackageDelay(recognizer.getLastPackageDelay())
                .build();
    }
    
    /**
     * 创建WordTimestamp
     */
    private SpeechRecognitionResponse.WordTimestamp createWordTimestamp(SpeechRecognitionResponse response) {
        return SpeechRecognitionResponse.WordTimestamp.builder()
                .beginTime(response.getBeginTime())
                .endTime(response.getEndTime())
                .text(response.getText())
                .build();
    }
    
    /**
     * 转换Word列表
     * 注意：由于SDK版本限制，暂时返回空列表，实际项目中可根据具体需求实现
     */
    private List<SpeechRecognitionResponse.WordTimestamp> convertWords(Object words) {
        // 由于SDK版本的API差异，暂时返回空列表
        // 在实际使用中，可以根据具体的SDK版本和API结构来实现词级别时间戳
        return List.of();
    }
    
    /**
     * 识别会话类
     */
    private static class RecognitionSession {
        private final String sessionId;
        private final Recognition recognizer;
        private final SseEmitter emitter;
        private final RecognitionParam param;
        private volatile boolean recognitionStarted = false;
        
        public RecognitionSession(Recognition recognizer, SseEmitter emitter, RecognitionParam param) {
            this.sessionId = java.util.UUID.randomUUID().toString();
            this.recognizer = recognizer;
            this.emitter = emitter;
            this.param = param;
        }
        
        // Getters and setters
        public String getSessionId() { return sessionId; }
        public Recognition getRecognizer() { return recognizer; }
        public SseEmitter getEmitter() { return emitter; }
        public RecognitionParam getParam() { return param; }
        public boolean isRecognitionStarted() { return recognitionStarted; }
        public void setRecognitionStarted(boolean recognitionStarted) { this.recognitionStarted = recognitionStarted; }
    }
}
