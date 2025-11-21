package com.carol.backend.service;

import com.carol.backend.dto.TtsSynthesisRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 流式TTS语音合成服务接口
 * 
 * @author jianjl
 * @version 1.0
 * @description 流式TTS语音合成服务
 * @date 2025-01-15
 */
public interface IStreamingTtsSynthesisService {
    
    /**
     * 创建流式语音合成会话
     * 
     * @param request TTS合成请求
     * @return SSE发射器
     */
    SseEmitter createStreamingSynthesis(TtsSynthesisRequest request);
    
    /**
     * 停止指定的流式合成会话
     * 
     * @param sessionId 会话ID
     * @return 是否成功
     */
    boolean stopStreamingSynthesis(String sessionId);
    
    /**
     * 获取活动会话数量
     * 
     * @return 活动会话数量
     */
    int getActiveSessionCount();
    
    /**
     * 清理所有活动会话
     */
    void cleanupAllSessions();
}
