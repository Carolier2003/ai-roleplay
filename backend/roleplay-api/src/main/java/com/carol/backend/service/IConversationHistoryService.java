package com.carol.backend.service;

import com.carol.backend.dto.ConversationMessageVO;
import com.carol.backend.dto.ChatHistoryResponse;

import java.util.List;

/**
 * 聊天历史服务接口
 * 
 * @author jianjl
 * @version 1.0
 * @description 聊天历史相关的业务逻辑服务
 * @date 2025-01-15
 */
public interface IConversationHistoryService {
    
    /**
     * 获取聊天历史 - 优先从Redis读取
     */
    ChatHistoryResponse getChatHistory(Long characterId, Long userId, String conversationId);
    
    /**
     * 获取所有角色的聊天历史 - 从Redis读取所有会话
     */
    ChatHistoryResponse getAllChatHistory(Long userId);
    
    /**
     * 获取归档的历史记录 - 限制最多查询前30天
     */
    List<ConversationMessageVO> getArchivedHistory(Long characterId, Long userId, 
                                                  Long beforeTime, Integer limit);
    
    /**
     * 删除消息 - Redis优先，MySQL逻辑删除
     */
    boolean deleteMessage(String messageId, Long characterId, Long userId);
    
    /**
     * 清空对话 - Redis和MySQL都处理
     */
    boolean clearConversation(Long characterId, Long userId);
    
    /**
     * 清空所有对话 - 清空用户的所有角色对话记录
     */
    boolean clearAllConversations(Long userId);
    
    /**
     * 归档Redis数据到MySQL
     */
    void archiveRedisDataToMySQL();
}
