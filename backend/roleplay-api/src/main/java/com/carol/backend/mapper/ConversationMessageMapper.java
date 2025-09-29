package com.carol.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carol.backend.entity.ConversationMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 对话消息Mapper
 * 
 * @author carol
 */
@Mapper
public interface ConversationMessageMapper extends BaseMapper<ConversationMessage> {
    
    /**
     * 根据会话ID查询消息列表
     */
    @Select("SELECT * FROM conversation_messages " +
            "WHERE conversation_id = #{conversationId} " +
            "ORDER BY message_index ASC")
    List<ConversationMessage> selectByConversationId(@Param("conversationId") Long conversationId);
    
    /**
     * 根据会话标识查询消息列表
     */
    @Select("SELECT * FROM conversation_messages " +
            "WHERE session_id = #{sessionId} " +
            "ORDER BY message_index ASC")
    List<ConversationMessage> selectBySessionId(@Param("sessionId") String sessionId);
    
    /**
     * 查询消息统计信息
     */
    @Select("SELECT " +
            "message_type, " +
            "COUNT(*) as message_count, " +
            "AVG(content_length) as avg_content_length, " +
            "SUM(token_count) as total_tokens, " +
            "AVG(response_time_ms) as avg_response_time " +
            "FROM conversation_messages " +
            "WHERE conversation_id = #{conversationId} " +
            "GROUP BY message_type")
    List<Map<String, Object>> getMessageStats(@Param("conversationId") Long conversationId);
    
    /**
     * 查询RAG知识使用统计
     */
    @Select("SELECT " +
            "rag_knowledge_used, " +
            "COUNT(*) as message_count, " +
            "AVG(rag_knowledge_count) as avg_knowledge_count " +
            "FROM conversation_messages " +
            "WHERE message_type = 2 " + // AI消息
            "GROUP BY rag_knowledge_used")
    List<Map<String, Object>> getRagUsageStats();
    
    /**
     * 批量插入消息
     */
    int insertBatch(@Param("messages") List<ConversationMessage> messages);
}
