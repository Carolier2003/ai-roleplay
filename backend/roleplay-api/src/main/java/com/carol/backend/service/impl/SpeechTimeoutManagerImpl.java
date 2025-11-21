package com.carol.backend.service.impl;

import com.carol.backend.config.SpeechPerformanceConfig;
import com.carol.backend.service.ISpeechTimeoutManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * 语音识别超时管理器实现类
 * 
 * @author jianjl
 * @version 1.0
 * @description 语音识别超时管理器实现
 * @date 2025-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechTimeoutManagerImpl implements ISpeechTimeoutManager {
    
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
    
    @Override
    public <T> T executeWithTimeout(Supplier<T> task, String taskType) throws TimeoutException {
        int timeoutSeconds = getTimeoutForTaskType(taskType);
        
        log.debug("[executeWithTimeout] 执行带超时的任务: taskType={}, timeoutSeconds={}", taskType, timeoutSeconds);
        
        CompletableFuture<T> future = CompletableFuture.supplyAsync(task, executorService);
        
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            log.warn("[executeWithTimeout] 任务执行超时: taskType={}, timeoutSeconds={}", taskType, timeoutSeconds);
            throw new ISpeechTimeoutManager.TimeoutException("任务执行超时: " + timeoutSeconds + "秒");
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
    
    @Override
    public <T> CompletableFuture<T> executeAsyncWithTimeout(Supplier<T> task, String taskType) {
        log.debug("[executeAsyncWithTimeout] 执行带超时的异步任务: taskType={}", taskType);
        int timeoutSeconds = getTimeoutForTaskType(taskType);
        
        CompletableFuture<T> future = CompletableFuture.supplyAsync(task, executorService);
        
        // 设置超时
        CompletableFuture<T> timeoutFuture = new CompletableFuture<>();
        ScheduledFuture<?> timeoutTask = scheduledExecutor.schedule(() -> {
            if (!future.isDone()) {
                future.cancel(true);
                timeoutFuture.completeExceptionally(
                    new ISpeechTimeoutManager.TimeoutException("异步任务执行超时: " + timeoutSeconds + "秒")
                );
                log.warn("[executeAsyncWithTimeout] 异步任务执行超时: taskType={}, timeoutSeconds={}", taskType, timeoutSeconds);
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
                log.warn("[getTimeoutForTaskType] 未知任务类型: taskType={}, 使用默认超时时间", taskType);
                return performanceConfig.getTimeout().getSyncRecognitionTimeoutSeconds();
        }
    }
    
    /**
     * 会话超时管理器实现
     */
    public class SessionTimeoutManager implements ISpeechTimeoutManager.SessionTimeoutManager {
        private final String sessionId;
        private final int timeoutSeconds;
        private volatile ScheduledFuture<?> timeoutTask;
        private volatile boolean cancelled = false;
        
        public SessionTimeoutManager(String sessionId, int timeoutSeconds) {
            this.sessionId = sessionId;
            this.timeoutSeconds = timeoutSeconds;
        }
        
        @Override
        public void startTimeout(Runnable onTimeout) {
            if (cancelled) {
                return;
            }
            
            log.debug("[SessionTimeoutManager.startTimeout] 启动会话超时检查: sessionId={}, timeoutSeconds={}", sessionId, timeoutSeconds);
            
            timeoutTask = scheduledExecutor.schedule(() -> {
                if (!cancelled) {
                    log.warn("[SessionTimeoutManager.startTimeout] 会话超时: sessionId={}, timeoutSeconds={}", sessionId, timeoutSeconds);
                    try {
                        onTimeout.run();
                    } catch (Exception e) {
                        log.error("[SessionTimeoutManager.startTimeout] 执行超时回调时发生错误: sessionId={}, error={}", sessionId, e.getMessage(), e);
                    }
                }
            }, timeoutSeconds, TimeUnit.SECONDS);
        }
        
        @Override
        public void resetTimeout(Runnable onTimeout) {
            cancelTimeout();
            startTimeout(onTimeout);
        }
        
        @Override
        public void cancelTimeout() {
            cancelled = true;
            if (timeoutTask != null && !timeoutTask.isDone()) {
                timeoutTask.cancel(false);
                log.debug("[SessionTimeoutManager.cancelTimeout] 已取消会话超时检查: sessionId={}", sessionId);
            }
        }
        
        @Override
        public boolean isTimedOut() {
            return timeoutTask != null && timeoutTask.isDone() && !cancelled;
        }
        
        @Override
        public long getRemainingTimeSeconds() {
            if (timeoutTask == null || timeoutTask.isDone()) {
                return 0;
            }
            return timeoutTask.getDelay(TimeUnit.SECONDS);
        }
    }
    
    
    @Override
    public void shutdown() {
        log.info("[shutdown] 关闭超时管理器");
        
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
