package com.carol.backend.service;

import com.alibaba.dashscope.exception.NoApiKeyException;
import com.carol.backend.dto.SpeechRecognitionRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 流式语音识别服务接口
 * 
 * @author jianjl
 * @version 1.0
 * @description 流式语音识别服务
 * @date 2025-01-15
 */
public interface IStreamingSpeechRecognitionService {
    
    /**
     * 创建流式识别会话
     * 
     * @param request 识别请求
     * @param emitter SSE发射器
     * @return 会话ID
     * @throws NoApiKeyException API密钥异常
     */
    String createStreamingSession(SpeechRecognitionRequest request, SseEmitter emitter) 
            throws NoApiKeyException;
    
    /**
     * 发送音频数据
     * 
     * @param sessionId 会话ID
     * @param audioData 音频数据
     */
    void sendAudioData(String sessionId, byte[] audioData);
    
    /**
     * 停止识别
     * 
     * @param sessionId 会话ID
     */
    void stopRecognition(String sessionId);
    
    /**
     * 关闭会话
     * 
     * @param sessionId 会话ID
     */
    void closeSession(String sessionId);
    
    /**
     * 获取活跃会话数量
     * 
     * @return 活跃会话数量
     */
    int getActiveSessionCount();
}
