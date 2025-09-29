package com.carol.backend.scheduler;

import com.carol.backend.service.IDataSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 数据同步定时任务调度器
 * 负责定时执行Redis到MySQL的数据同步任务
 */
@Slf4j
@Component
public class DataSyncScheduler {
    
    @Autowired
    private IDataSyncService dataSyncService;
    
    /**
     * 每天晚上6点执行数据同步任务
     * cron表达式: 0 0 18 * * ?
     * 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 18 * * ?")
    public void scheduledDataSync() {
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        log.info("[scheduledDataSync] 定时数据同步任务开始执行，当前时间: {}", currentTime);
        
        try {
            // 执行完整的数据同步
            IDataSyncService.DataSyncResult result = dataSyncService.performFullDataSync();
            
            if (result.isSuccess()) {
                log.info("[scheduledDataSync] 定时数据同步任务执行成功: " +
                        "消息{}条, 会话{}个, 用户{}个, 耗时{}ms", 
                        result.getSyncedMessages(), 
                        result.getSyncedConversations(), 
                        result.getSyncedUsers(), 
                        result.getSyncDuration());
            } else {
                log.error("[scheduledDataSync] 定时数据同步任务执行失败: {}", result.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("[scheduledDataSync] 定时数据同步任务执行异常: {}", e.getMessage(), e);
        }
        
        log.info("[scheduledDataSync] 定时数据同步任务执行完成");
    }
    
    /**
     * 每小时执行一次增量同步（可选）
     * cron表达式: 0 0 * * * ?
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void hourlyIncrementalSync() {
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        log.info("[hourlyIncrementalSync] 每小时增量同步任务开始执行，当前时间: {}", currentTime);
        
        try {
            // 只同步聊天消息（增量同步）
            int syncedMessages = dataSyncService.syncChatMessagesToMysql();
            
            log.info("[hourlyIncrementalSync] 增量同步完成，同步消息 {} 条", syncedMessages);
            
        } catch (Exception e) {
            log.error("[hourlyIncrementalSync] 增量同步任务执行异常: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 每天凌晨2点执行数据清理任务（可选）
     * cron表达式: 0 0 2 * * ?
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyDataCleanup() {
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        log.info("[dailyDataCleanup] 每日数据清理任务开始执行，当前时间: {}", currentTime);
        
        try {
            // 这里可以添加数据清理逻辑
            // 例如：清理过期的Redis数据、压缩历史数据等
            
            log.info("[dailyDataCleanup] 数据清理任务执行完成");
            
        } catch (Exception e) {
            log.error("[dailyDataCleanup] 数据清理任务执行异常: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 手动触发数据同步（用于测试）
     * 可以通过API接口调用
     */
    public IDataSyncService.DataSyncResult manualDataSync() {
        log.info("[manualDataSync] 手动触发数据同步任务");
        
        try {
            IDataSyncService.DataSyncResult result = dataSyncService.performFullDataSync();
            
            log.info("[manualDataSync] 手动数据同步任务完成: {}", result);
            
            return result;
            
        } catch (Exception e) {
            log.error("[manualDataSync] 手动数据同步任务异常: {}", e.getMessage(), e);
            
            IDataSyncService.DataSyncResult errorResult = new IDataSyncService.DataSyncResult();
            errorResult.setSuccess(false);
            errorResult.setErrorMessage(e.getMessage());
            
            return errorResult;
        }
    }
}
