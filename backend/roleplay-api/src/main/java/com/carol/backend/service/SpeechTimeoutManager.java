package com.carol.backend.service;

import com.carol.backend.config.SpeechPerformanceConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * 语音识别超时管理器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechTimeoutManager {
    
    private final SpeechPerformanceConfig performanceConfig;
    
    // 线程池
    private final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "speech-timeout-" + System.currentTimeMillis());
        thread.setDaemon(true);
        return thread;
    });
    
    // 定时任务调度器
    private final ScheduledExecutorService scheduledExecutor = 
            Executors.newScheduledThreadPool(2, r -> {
                Thread thread = new Thread(r, "speech-timeout-scheduler-" + System.currentTimeMillis());
                thread.setDaemon(true);
                return thread;
            });
    
    /**
     * 执行带超时的同步识别任务
     */
    public <T> T executeWithTimeout(Supplier<T> task, String taskType) throws TimeoutException {
        int timeoutSeconds = getTimeoutForTaskType(taskType);
        
        log.debug("执行带超时的任务: type={}, timeout={}s", taskType, timeoutSeconds);
        
        CompletableFuture<T> future = CompletableFuture.supplyAsync(task, executorService);
        
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            log.warn("任务执行超时: type={}, timeout={}s", taskType, timeoutSeconds);
            throw new TimeoutException("任务执行超时: " + timeoutSeconds + "秒");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("任务被中断", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new RuntimeException("任务执行失败", cause);
            }
        }
    }
    
    /**
     * 执行带超时的异步识别任务
     */
    public <T> CompletableFuture<T> executeAsyncWithTimeout(Supplier<T> task, String taskType) {
        int timeoutSeconds = getTimeoutForTaskType(taskType);
        
        log.debug("执行带超时的异步任务: type={}, timeout={}s", taskType, timeoutSeconds);
        
        CompletableFuture<T> future = CompletableFuture.supplyAsync(task, executorService);
        
        // 设置超时
        CompletableFuture<T> timeoutFuture = new CompletableFuture<>();
        ScheduledFuture<?> timeoutTask = scheduledExecutor.schedule(() -> {
            if (!future.isDone()) {
                future.cancel(true);
                timeoutFuture.completeExceptionally(
                    new TimeoutException("异步任务执行超时: " + timeoutSeconds + "秒")
                );
                log.warn("异步任务执行超时: type={}, timeout={}s", taskType, timeoutSeconds);
            }
        }, timeoutSeconds, TimeUnit.SECONDS);
        
        // 当原任务完成时，取消超时任务
        future.whenComplete((result, exception) -> {
            timeoutTask.cancel(false);
            if (exception != null) {
                timeoutFuture.completeExceptionally(exception);
            } else {
                timeoutFuture.complete(result);
            }
        });
        
        return timeoutFuture;
    }
    
    /**
     * 创建带超时的会话管理器
     */
    public SessionTimeoutManager createSessionTimeoutManager(String sessionId) {
        return new SessionTimeoutManager(sessionId, performanceConfig.getTimeout().getStreamingSessionTimeoutSeconds());
    }
    
    /**
     * 根据任务类型获取超时时间
     */
    private int getTimeoutForTaskType(String taskType) {
        switch (taskType.toLowerCase()) {
            case "sync":
            case "synchronous":
                return performanceConfig.getTimeout().getSyncRecognitionTimeoutSeconds();
            case "async":
            case "asynchronous":
                return performanceConfig.getTimeout().getAsyncRecognitionTimeoutSeconds();
            case "streaming":
                return performanceConfig.getTimeout().getStreamingSessionTimeoutSeconds();
            case "connection":
                return performanceConfig.getTimeout().getConnectionTimeoutSeconds();
            case "read":
                return performanceConfig.getTimeout().getReadTimeoutSeconds();
            default:
                log.warn("未知任务类型: {}, 使用默认超时时间", taskType);
                return performanceConfig.getTimeout().getSyncRecognitionTimeoutSeconds();
        }
    }
    
    /**
     * 会话超时管理器
     */
    public class SessionTimeoutManager {
        private final String sessionId;
        private final int timeoutSeconds;
        private volatile ScheduledFuture<?> timeoutTask;
        private volatile boolean cancelled = false;
        
        public SessionTimeoutManager(String sessionId, int timeoutSeconds) {
            this.sessionId = sessionId;
            this.timeoutSeconds = timeoutSeconds;
        }
        
        /**
         * 启动超时检查
         */
        public void startTimeout(Runnable onTimeout) {
            if (cancelled) {
                return;
            }
            
            log.debug("启动会话超时检查: sessionId={}, timeout={}s", sessionId, timeoutSeconds);
            
            timeoutTask = scheduledExecutor.schedule(() -> {
                if (!cancelled) {
                    log.warn("会话超时: sessionId={}, timeout={}s", sessionId, timeoutSeconds);
                    try {
                        onTimeout.run();
                    } catch (Exception e) {
                        log.error("执行超时回调时发生错误: sessionId=" + sessionId, e);
                    }
                }
            }, timeoutSeconds, TimeUnit.SECONDS);
        }
        
        /**
         * 重置超时时间
         */
        public void resetTimeout(Runnable onTimeout) {
            cancelTimeout();
            startTimeout(onTimeout);
        }
        
        /**
         * 取消超时检查
         */
        public void cancelTimeout() {
            cancelled = true;
            if (timeoutTask != null && !timeoutTask.isDone()) {
                timeoutTask.cancel(false);
                log.debug("已取消会话超时检查: sessionId={}", sessionId);
            }
        }
        
        /**
         * 检查是否已超时
         */
        public boolean isTimedOut() {
            return timeoutTask != null && timeoutTask.isDone() && !cancelled;
        }
        
        /**
         * 获取剩余时间（秒）
         */
        public long getRemainingTimeSeconds() {
            if (timeoutTask == null || timeoutTask.isDone()) {
                return 0;
            }
            return timeoutTask.getDelay(TimeUnit.SECONDS);
        }
    }
    
    /**
     * 超时异常
     */
    public static class TimeoutException extends Exception {
        public TimeoutException(String message) {
            super(message);
        }
        
        public TimeoutException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * 关闭超时管理器
     */
    public void shutdown() {
        log.info("关闭语音识别超时管理器");
        
        executorService.shutdown();
        scheduledExecutor.shutdown();
        
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
            scheduledExecutor.shutdownNow();
        }
    }
}
