package com.carol.backend.service.impl;

import com.carol.backend.config.SpeechPerformanceConfig;
import com.carol.backend.service.ISpeechResourceManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * 语音识别资源管理器实现类
 * 
 * @author jianjl
 * @version 1.0
 * @description 负责线程池管理、临时文件清理、内存优化等
 * @date 2025-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechResourceManagerImpl implements ISpeechResourceManager {
    
    private final SpeechPerformanceConfig performanceConfig;
    
    // 同步任务线程池
    private ThreadPoolExecutor syncExecutor;
    
    // 异步任务线程池
    private ThreadPoolExecutor asyncExecutor;
    
    // 当前活跃任务计数
    private final AtomicInteger activeSyncTasks = new AtomicInteger(0);
    private final AtomicInteger activeAsyncTasks = new AtomicInteger(0);
    private final AtomicInteger activeStreamingSessions = new AtomicInteger(0);
    
    @Override
    public void init() {
        log.info("[init] 初始化语音识别资源管理器");
        
        // 创建同步任务线程池
        syncExecutor = new ThreadPoolExecutor(
            performanceConfig.getConcurrency().getCorePoolSize(),
            performanceConfig.getConcurrency().getMaxPoolSize(),
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(performanceConfig.getConcurrency().getQueueSize()),
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(0);
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "speech-sync-" + counter.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        // 创建异步任务线程池
        asyncExecutor = new ThreadPoolExecutor(
            performanceConfig.getConcurrency().getCorePoolSize(),
            performanceConfig.getConcurrency().getMaxAsyncTasks(),
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(performanceConfig.getConcurrency().getQueueSize()),
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(0);
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "speech-async-" + counter.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        log.info("[init] 线程池初始化完成: sync=[{}-{}], async=[{}-{}]",
            performanceConfig.getConcurrency().getCorePoolSize(),
            performanceConfig.getConcurrency().getMaxPoolSize(),
            performanceConfig.getConcurrency().getCorePoolSize(),
            performanceConfig.getConcurrency().getMaxAsyncTasks());
    }
    
    @Override
    public <T> Future<T> submitSyncTask(Callable<T> task) throws RejectedExecutionException {
        log.debug("[submitSyncTask] 提交同步任务");
        if (activeSyncTasks.get() >= performanceConfig.getConcurrency().getMaxSyncTasks()) {
            throw new RejectedExecutionException("同步任务达到最大并发限制: " + 
                performanceConfig.getConcurrency().getMaxSyncTasks());
        }
        
        activeSyncTasks.incrementAndGet();
        
        return syncExecutor.submit(() -> {
            try {
                return task.call();
            } finally {
                activeSyncTasks.decrementAndGet();
            }
        });
    }
    
    @Override
    public <T> CompletableFuture<T> submitAsyncTask(Callable<T> task) throws RejectedExecutionException {
        log.debug("[submitAsyncTask] 提交异步任务");
        if (activeAsyncTasks.get() >= performanceConfig.getConcurrency().getMaxAsyncTasks()) {
            throw new RejectedExecutionException("异步任务达到最大并发限制: " + 
                performanceConfig.getConcurrency().getMaxAsyncTasks());
        }
        
        activeAsyncTasks.incrementAndGet();
        
        CompletableFuture<T> future = new CompletableFuture<>();
        
        asyncExecutor.execute(() -> {
            try {
                T result = task.call();
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            } finally {
                activeAsyncTasks.decrementAndGet();
            }
        });
        
        return future;
    }
    
    @Override
    public boolean canCreateStreamingSession() {
        boolean canCreate = activeStreamingSessions.get() < performanceConfig.getConcurrency().getMaxStreamingSessions();
        log.debug("[canCreateStreamingSession] 检查是否可以创建流式会话: canCreate={}, currentCount={}, maxCount={}", 
                canCreate, activeStreamingSessions.get(), performanceConfig.getConcurrency().getMaxStreamingSessions());
        return canCreate;
    }
    
    @Override
    public void incrementStreamingSession() {
        int count = activeStreamingSessions.incrementAndGet();
        log.debug("[incrementStreamingSession] 增加流式会话计数: count={}", count);
    }
    
    @Override
    public void decrementStreamingSession() {
        int count = activeStreamingSessions.decrementAndGet();
        log.debug("[decrementStreamingSession] 减少流式会话计数: count={}", count);
    }
    
    @Override
    public File createTempFile(String prefix, String suffix) throws IOException {
        log.debug("[createTempFile] 创建临时文件: prefix={}, suffix={}", prefix, suffix);
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "speech-recognition");
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }
        
        String fileName = String.format("%s_%d_%d%s", 
            prefix, 
            System.currentTimeMillis(), 
            Thread.currentThread().getId(),
            suffix);
        
        File tempFile = tempDir.resolve(fileName).toFile();
        tempFile.deleteOnExit();
        
        log.debug("[createTempFile] 临时文件创建成功: path={}", tempFile.getPath());
        return tempFile;
    }
    
    /**
     * 定期清理临时文件
     */
    @Scheduled(fixedRate = 300000) // 每5分钟执行一次
    public void cleanupTempFiles() {
        log.debug("[cleanupTempFiles] 开始清理临时文件");
        try {
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "speech-recognition");
            if (!Files.exists(tempDir)) {
                return;
            }
            
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            
            try (Stream<Path> files = Files.list(tempDir)) {
                long deletedCount = files
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        try {
                            LocalDateTime fileTime = LocalDateTime.ofInstant(
                                Files.getLastModifiedTime(path).toInstant(),
                                ZoneId.systemDefault()
                            );
                            return fileTime.isBefore(oneHourAgo);
                        } catch (IOException e) {
                            log.warn("无法获取文件修改时间: {}", path, e);
                            return false;
                        }
                    })
                    .peek(path -> {
                        try {
                            Files.delete(path);
                            log.debug("删除过期临时文件: {}", path);
                        } catch (IOException e) {
                            log.warn("删除临时文件失败: {}", path, e);
                        }
                    })
                    .count();
                
                if (deletedCount > 0) {
                    log.info("清理了 {} 个过期临时文件", deletedCount);
                }
            }
            
        } catch (Exception e) {
            log.error("清理临时文件时发生错误", e);
        }
    }
    
    @Override
    public ISpeechResourceManager.ResourceUsageStats getResourceUsageStats() {
        log.debug("[getResourceUsageStats] 获取资源使用统计");
        ISpeechResourceManager.ResourceUsageStats stats = new ISpeechResourceManager.ResourceUsageStats();
        
        // 线程池统计
        stats.setSyncActiveThreads(syncExecutor.getActiveCount());
        stats.setSyncQueueSize(syncExecutor.getQueue().size());
        stats.setAsyncActiveThreads(asyncExecutor.getActiveCount());
        stats.setAsyncQueueSize(asyncExecutor.getQueue().size());
        
        // 任务统计
        stats.setActiveSyncTasks(activeSyncTasks.get());
        stats.setActiveAsyncTasks(activeAsyncTasks.get());
        stats.setActiveStreamingSessions(activeStreamingSessions.get());
        
        // 内存统计
        Runtime runtime = Runtime.getRuntime();
        stats.setTotalMemoryMB(runtime.totalMemory() / (1024 * 1024));
        stats.setFreeMemoryMB(runtime.freeMemory() / (1024 * 1024));
        stats.setUsedMemoryMB((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
        stats.setMaxMemoryMB(runtime.maxMemory() / (1024 * 1024));
        
        return stats;
    }
    
    @Override
    public void forceGC() {
        log.info("[forceGC] 执行强制垃圾回收");
        System.gc();
        
        // 记录GC后的内存状态
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        log.info("[forceGC] GC完成，当前内存使用: usedMemory={}MB", usedMemory);
    }
    
    @Override
    public void shutdown() {
        log.info("[shutdown] 关闭语音识别资源管理器");
        
        if (syncExecutor != null) {
            syncExecutor.shutdown();
            try {
                if (!syncExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    syncExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                syncExecutor.shutdownNow();
            }
        }
        
        if (asyncExecutor != null) {
            asyncExecutor.shutdown();
            try {
                if (!asyncExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    asyncExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                asyncExecutor.shutdownNow();
            }
        }
        
        log.info("资源管理器关闭完成");
    }
    
}
