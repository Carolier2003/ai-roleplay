package com.carol.backend.service;

/**
 * 数据同步服务接口
 * 负责将Redis中的数据同步到MySQL数据库
 */
public interface IDataSyncService {
    
    /**
     * 同步所有用户的聊天记录从Redis到MySQL
     * @return 同步成功的记录数量
     */
    int syncChatMessagesToMysql();
    
    /**
     * 同步所有用户的会话信息从Redis到MySQL
     * @return 同步成功的会话数量
     */
    int syncConversationsToMysql();
    
    /**
     * 同步用户活动数据从Redis到MySQL
     * @return 同步成功的用户数量
     */
    int syncUserActivityToMysql();
    
    /**
     * 执行完整的数据同步任务
     * 包括聊天记录、会话信息、用户活动等所有数据
     * @return 同步结果摘要
     */
    DataSyncResult performFullDataSync();
    
    /**
     * 数据同步结果
     */
    class DataSyncResult {
        private int syncedMessages;
        private int syncedConversations;
        private int syncedUsers;
        private long syncDuration;
        private boolean success;
        private String errorMessage;
        
        // 构造函数
        public DataSyncResult() {}
        
        public DataSyncResult(int syncedMessages, int syncedConversations, int syncedUsers, 
                            long syncDuration, boolean success, String errorMessage) {
            this.syncedMessages = syncedMessages;
            this.syncedConversations = syncedConversations;
            this.syncedUsers = syncedUsers;
            this.syncDuration = syncDuration;
            this.success = success;
            this.errorMessage = errorMessage;
        }
        
        // Getters and Setters
        public int getSyncedMessages() {
            return syncedMessages;
        }
        
        public void setSyncedMessages(int syncedMessages) {
            this.syncedMessages = syncedMessages;
        }
        
        public int getSyncedConversations() {
            return syncedConversations;
        }
        
        public void setSyncedConversations(int syncedConversations) {
            this.syncedConversations = syncedConversations;
        }
        
        public int getSyncedUsers() {
            return syncedUsers;
        }
        
        public void setSyncedUsers(int syncedUsers) {
            this.syncedUsers = syncedUsers;
        }
        
        public long getSyncDuration() {
            return syncDuration;
        }
        
        public void setSyncDuration(long syncDuration) {
            this.syncDuration = syncDuration;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
        
        @Override
        public String toString() {
            return String.format(
                "DataSyncResult{success=%s, messages=%d, conversations=%d, users=%d, duration=%dms, error='%s'}", 
                success, syncedMessages, syncedConversations, syncedUsers, syncDuration, errorMessage
            );
        }
    }
}
