package com.carol.backend.service;

import com.carol.backend.dto.QwenConversationInfo;
import com.carol.backend.dto.QwenConversationResponse;

import java.util.List;

/**
 * Qwen 会话管理服务
 * 
 * @author carol
 */
public interface QwenConversationService {
    
    /**
     * 创建新的 Qwen 会话
     * 
     * @param userId 用户ID
     * @return 会话响应（包含 conversationId）
     */
    QwenConversationResponse createConversation(Long userId);
    
    /**
     * 列出用户的所有 Qwen 会话
     * 
     * @param userId 用户ID
     * @return 会话列表（按最后活跃时间降序）
     */
    List<QwenConversationInfo> listConversations(Long userId);
    
    /**
     * 获取会话详情
     * 
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @return 会话信息
     */
    QwenConversationInfo getConversationInfo(Long userId, String conversationId);
    
    /**
     * 删除会话
     * 
     * @param userId 用户ID
     * @param conversationId 会话ID
     */
    void deleteConversation(Long userId, String conversationId);
    
    /**
     * 重命名会话
     * 
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @param title 新标题
     */
    void renameConversation(Long userId, String conversationId, String title);
    
    /**
     * 更新会话的最后活跃时间
     * 
     * @param userId 用户ID
     * @param conversationId 会话ID
     */
    void updateLastActiveTime(Long userId, String conversationId);
    
    /**
     * 自动生成会话标题（从首条消息提取）
     * 
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @param firstMessage 首条用户消息
     */
    void generateTitle(Long userId, String conversationId, String firstMessage);
}
