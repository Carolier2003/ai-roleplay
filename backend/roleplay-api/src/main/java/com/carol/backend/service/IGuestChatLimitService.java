package com.carol.backend.service;

/**
 * 游客聊天限制服务接口
 */
public interface IGuestChatLimitService {
    
    /**
     * 检查游客是否还能继续聊天
     * @param sessionId 会话ID（用于标识游客）
     * @return true-可以聊天，false-已达到限制
     */
    boolean canGuestChat(String sessionId);
    
    /**
     * 增加游客聊天次数
     * @param sessionId 会话ID
     */
    void incrementGuestChatCount(String sessionId);
    
    /**
     * 获取游客今日聊天次数
     * @param sessionId 会话ID
     * @return 聊天次数
     */
    int getGuestChatCount(String sessionId);
    
    /**
     * 游客最大聊天次数
     */
    int MAX_GUEST_CHAT_COUNT = 5;
}
