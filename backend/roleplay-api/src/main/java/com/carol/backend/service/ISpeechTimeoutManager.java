package com.carol.backend.service;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 语音识别超时管理器接口
 * 
 * @author jianjl
 * @version 1.0
 * @description 语音识别超时管理器
 * @date 2025-01-15
 */
public interface ISpeechTimeoutManager {
    
    /**
     * 执行带超时的同步识别任务
     * 
     * @param task 任务
     * @param taskType 任务类型
     * @param <T> 返回类型
     * @return 任务结果
     * @throws TimeoutException 超时异常
     */
    <T> T executeWithTimeout(Supplier<T> task, String taskType) throws TimeoutException;
    
    /**
     * 执行带超时的异步识别任务
     * 
     * @param task 任务
     * @param taskType 任务类型
     * @param <T> 返回类型
     * @return CompletableFuture
     */
    <T> CompletableFuture<T> executeAsyncWithTimeout(Supplier<T> task, String taskType);
    
    /**
     * 创建带超时的会话管理器
     * 
     * @param sessionId 会话ID
     * @return 会话超时管理器
     */
    SessionTimeoutManager createSessionTimeoutManager(String sessionId);
    
    /**
     * 关闭超时管理器
     */
    void shutdown();
    
    /**
     * 会话超时管理器
     */
    interface SessionTimeoutManager {
        void startTimeout(Runnable onTimeout);
        void resetTimeout(Runnable onTimeout);
        void cancelTimeout();
        boolean isTimedOut();
        long getRemainingTimeSeconds();
    }
    
    /**
     * 超时异常
     */
    class TimeoutException extends Exception {
        public TimeoutException(String message) {
            super(message);
        }
        
        public TimeoutException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
