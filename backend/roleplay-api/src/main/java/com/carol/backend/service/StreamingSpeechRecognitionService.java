package com.carol.backend.service;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.exception.NoApiKeyException;

import com.carol.backend.config.SpeechRecognitionConfig;
import com.carol.backend.dto.SpeechRecognitionRequest;
import com.carol.backend.dto.SpeechRecognitionResponse;
import com.carol.backend.dto.StreamingSpeechRecognitionResponse;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 流式语音识别服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingSpeechRecognitionService {
    
    private final SpeechRecognitionConfig config;
    
    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;
    
    /**
     * 初始化时设置全局API密钥
     */
    @PostConstruct
    private void initializeApiKey() {
        System.setProperty("dashscope.api-key", apiKey);
        log.debug("流式ASR服务已设置DashScope API密钥系统属性");
    }
    
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    // 管理活跃的识别会话
    private final Map<String, RecognitionSession> activeSessions = new ConcurrentHashMap<>();
    
    /**
     * 创建流式识别会话
     */
    public String createStreamingSession(SpeechRecognitionRequest request, SseEmitter emitter) 
            throws NoApiKeyException {
        
        log.info("创建流式语音识别会话");
        
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
            log.info("SSE连接完成，会话ID: {}", sessionId);
            closeSession(sessionId);
        });
        emitter.onTimeout(() -> {
            log.warn("SSE连接超时，会话ID: {}", sessionId);
            closeSession(sessionId);
        });
        emitter.onError(throwable -> {
            log.error("SSE连接错误，会话ID: {}", sessionId, throwable);
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
                    
                    log.debug("发送识别结果: {}", response.getText());
                    
                } catch (IOException e) {
                    log.error("发送SSE事件失败，会话ID: {}", sessionId, e);
                    closeSession(sessionId);
                }
            }
            
            @Override
            public void onComplete() {
                try {
                    log.info("流式识别完成，会话ID: {}", sessionId);
                    
                    // 标记识别已完成，避免重复停止
                    session.setRecognitionStarted(false);
                    
                    StreamingSpeechRecognitionResponse completeResponse = 
                        StreamingSpeechRecognitionResponse.complete(sessionId);
                    
                    emitter.send(SseEmitter.event()
                        .name("complete")
                        .data(completeResponse));
                        
                    emitter.complete();
                    
                } catch (IOException e) {
                    log.error("发送完成事件失败，会话ID: {}", sessionId, e);
                } finally {
                    closeSession(sessionId);
                }
            }
            
            @Override
            public void onError(Exception e) {
                try {
                    log.error("流式识别错误，会话ID: {}", sessionId, e);
                    
                    StreamingSpeechRecognitionResponse errorResponse = 
                        StreamingSpeechRecognitionResponse.error(sessionId, e.getMessage());
                    
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data(errorResponse));
                        
                    emitter.completeWithError(e);
                    
                } catch (IOException ioException) {
                    log.error("发送错误事件失败，会话ID: {}", sessionId, ioException);
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
    public void sendAudioData(String sessionId, byte[] audioData) {
        RecognitionSession session = activeSessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在: " + sessionId);
        }
        
        if (!session.isRecognitionStarted()) {
            throw new IllegalStateException("识别尚未启动");
        }
        
        try {
            ByteBuffer buffer = ByteBuffer.wrap(audioData);
            session.getRecognizer().sendAudioFrame(buffer);
            
            log.debug("发送音频数据，会话ID: {}, 数据大小: {} bytes", sessionId, audioData.length);
            
        } catch (Exception e) {
            log.error("发送音频数据失败，会话ID: {}", sessionId, e);
            throw new RuntimeException("发送音频数据失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 停止识别
     */
    public void stopRecognition(String sessionId) {
        RecognitionSession session = activeSessions.get(sessionId);
        if (session != null && session.isRecognitionStarted()) {
            try {
                session.getRecognizer().stop();
                log.info("识别已停止，会话ID: {}", sessionId);
            } catch (Exception e) {
                log.error("停止识别失败，会话ID: {}", sessionId, e);
            }
        }
    }
    
    /**
     * 关闭会话
     */
    public void closeSession(String sessionId) {
        RecognitionSession session = activeSessions.remove(sessionId);
        if (session != null) {
            try {
                // 停止识别 - 只有在识别已启动且未完成的情况下才调用stop
                if (session.isRecognitionStarted()) {
                    try {
                        session.getRecognizer().stop();
                        log.debug("识别器已停止，会话ID: {}", sessionId);
                    } catch (Exception stopException) {
                        // 如果识别器已经自动停止，忽略状态错误
                        if (stopException.getMessage() != null && 
                            stopException.getMessage().contains("State invalid")) {
                            log.debug("识别器已自动停止，跳过手动停止，会话ID: {}", sessionId);
                        } else {
                            throw stopException;
                        }
                    }
                }
                
                // 关闭WebSocket连接
                try {
                    session.getRecognizer().getDuplexApi().close(1000, "会话结束");
                    log.debug("WebSocket连接已关闭，会话ID: {}", sessionId);
                } catch (Exception closeException) {
                    // WebSocket可能已经关闭，记录但不抛出异常
                    log.debug("WebSocket连接关闭时出现异常（可能已关闭），会话ID: {}, 异常: {}", 
                             sessionId, closeException.getMessage());
                }
                
                log.info("会话已关闭，会话ID: {}", sessionId);
                
            } catch (Exception e) {
                log.error("关闭会话失败，会话ID: {}", sessionId, e);
            }
        } else {
            log.debug("会话不存在或已关闭，会话ID: {}", sessionId);
        }
    }
    
    /**
     * 获取活跃会话数量
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
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
