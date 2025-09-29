package com.carol.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

/**
 * 流式响应配置类
 * 确保Server-Sent Events (SSE) 使用正确的UTF-8编码
 * 
 * @author carol
 */
@Slf4j
@Configuration
public class StreamingConfig {

    /**
     * 配置SSE响应编码
     */
    @Bean
    public SseEmitter.SseEventBuilder sseEventBuilder() {
        log.info("[sseEventBuilder] 配置SSE事件构建器，使用UTF-8编码");
        
        return SseEmitter.event()
                .reconnectTime(3000L); // 设置重连时间
    }
    
    /**
     * 自定义响应体发射器，确保UTF-8编码
     */
    public static void configureStreamingResponse(HttpServletResponse response) {
        log.debug("[configureStreamingResponse] 配置流式响应头，使用UTF-8编码");
        
        // 设置响应头，确保UTF-8编码
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/event-stream;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Headers", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    }
}
