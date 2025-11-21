package com.carol.backend.service;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/**
 * 语音识别资源管理器接口
 * 
 * @author jianjl
 * @version 1.0
 * @description 负责线程池管理、临时文件清理、内存优化等
 * @date 2025-01-15
 */
public interface ISpeechResourceManager {
    
    /**
     * 初始化资源管理器
     */
    void init();
    
    /**
     * 提交同步任务
     * 
     * @param task 任务
     * @param <T> 返回类型
     * @return Future
     * @throws RejectedExecutionException 拒绝执行异常
     */
    <T> Future<T> submitSyncTask(Callable<T> task) throws RejectedExecutionException;
    
    /**
     * 提交异步任务
     * 
     * @param task 任务
     * @param <T> 返回类型
     * @return CompletableFuture
     * @throws RejectedExecutionException 拒绝执行异常
     */
    <T> CompletableFuture<T> submitAsyncTask(Callable<T> task) throws RejectedExecutionException;
    
    /**
     * 检查是否可以创建新的流式会话
     * 
     * @return 是否可以创建
     */
    boolean canCreateStreamingSession();
    
    /**
     * 增加流式会话计数
     */
    void incrementStreamingSession();
    
    /**
     * 减少流式会话计数
     */
    void decrementStreamingSession();
    
    /**
     * 创建临时文件
     * 
     * @param prefix 前缀
     * @param suffix 后缀
     * @return 临时文件
     * @throws IOException IO异常
     */
    File createTempFile(String prefix, String suffix) throws IOException;
    
    /**
     * 获取资源使用统计
     * 
     * @return 资源使用统计
     */
    ResourceUsageStats getResourceUsageStats();
    
    /**
     * 强制垃圾回收
     */
    void forceGC();
    
    /**
     * 关闭资源管理器
     */
    void shutdown();
    
    /**
     * 资源使用统计
     */
    class ResourceUsageStats {
        private int syncActiveThreads;
        private int syncQueueSize;
        private int asyncActiveThreads;
        private int asyncQueueSize;
        private int activeSyncTasks;
        private int activeAsyncTasks;
        private int activeStreamingSessions;
        private long totalMemoryMB;
        private long freeMemoryMB;
        private long usedMemoryMB;
        private long maxMemoryMB;
        
        // Getters and setters
        public int getSyncActiveThreads() { return syncActiveThreads; }
        public void setSyncActiveThreads(int syncActiveThreads) { this.syncActiveThreads = syncActiveThreads; }
        
        public int getSyncQueueSize() { return syncQueueSize; }
        public void setSyncQueueSize(int syncQueueSize) { this.syncQueueSize = syncQueueSize; }
        
        public int getAsyncActiveThreads() { return asyncActiveThreads; }
        public void setAsyncActiveThreads(int asyncActiveThreads) { this.asyncActiveThreads = asyncActiveThreads; }
        
        public int getAsyncQueueSize() { return asyncQueueSize; }
        public void setAsyncQueueSize(int asyncQueueSize) { this.asyncQueueSize = asyncQueueSize; }
        
        public int getActiveSyncTasks() { return activeSyncTasks; }
        public void setActiveSyncTasks(int activeSyncTasks) { this.activeSyncTasks = activeSyncTasks; }
        
        public int getActiveAsyncTasks() { return activeAsyncTasks; }
        public void setActiveAsyncTasks(int activeAsyncTasks) { this.activeAsyncTasks = activeAsyncTasks; }
        
        public int getActiveStreamingSessions() { return activeStreamingSessions; }
        public void setActiveStreamingSessions(int activeStreamingSessions) { this.activeStreamingSessions = activeStreamingSessions; }
        
        public long getTotalMemoryMB() { return totalMemoryMB; }
        public void setTotalMemoryMB(long totalMemoryMB) { this.totalMemoryMB = totalMemoryMB; }
        
        public long getFreeMemoryMB() { return freeMemoryMB; }
        public void setFreeMemoryMB(long freeMemoryMB) { this.freeMemoryMB = freeMemoryMB; }
        
        public long getUsedMemoryMB() { return usedMemoryMB; }
        public void setUsedMemoryMB(long usedMemoryMB) { this.usedMemoryMB = usedMemoryMB; }
        
        public long getMaxMemoryMB() { return maxMemoryMB; }
        public void setMaxMemoryMB(long maxMemoryMB) { this.maxMemoryMB = maxMemoryMB; }
    }
}
