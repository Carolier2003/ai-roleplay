package com.carol.backend.service;

import java.util.Map;

/**
 * 对话同步服务接口
 * 
 * @author jianjl
 * @version 1.0
 * @description 直接从Redis读取数据同步到MySQL数据仓库
 * @date 2025-01-15
 */
public interface IConversationSyncService {
    
    /**
     * 同步指定会话的对话数据
     * 
     * @param sessionId 会话ID
     * @return 是否成功
     */
    boolean syncConversation(String sessionId);
    
    /**
     * 批量同步所有Redis中的对话
     * 
     * @return 同步结果统计
     */
    Map<String, Object> syncAllConversations();
}
