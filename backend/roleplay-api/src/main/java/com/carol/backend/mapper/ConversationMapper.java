package com.carol.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carol.backend.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 对话会话Mapper
 * 
 * @author carol
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
    
    /**
     * 根据会话ID查询对话
     */
    @Select("SELECT * FROM conversations WHERE session_id = #{sessionId}")
    Conversation selectBySessionId(@Param("sessionId") String sessionId);
    
    /**
     * 查询指定时间范围内的对话统计
     */
    @Select("SELECT " +
            "character_id, " +
            "COUNT(*) as conversation_count, " +
            "AVG(message_count) as avg_message_count, " +
            "SUM(total_tokens) as total_tokens, " +
            "AVG(duration_minutes) as avg_duration " +
            "FROM conversations " +
            "WHERE start_time >= #{startTime} AND start_time <= #{endTime} " +
            "AND sync_status = 1 " +
            "GROUP BY character_id")
    List<Map<String, Object>> getConversationStats(@Param("startTime") LocalDateTime startTime, 
                                                   @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询需要同步的Redis会话（检查是否已存在）
     */
    @Select("SELECT COUNT(*) FROM conversations WHERE session_id = #{sessionId}")
    int countBySessionId(@Param("sessionId") String sessionId);
    
    /**
     * 获取对话分析视图数据
     */
    @Select("SELECT * FROM conversation_analytics " +
            "WHERE conversation_date >= #{startDate} " +
            "AND conversation_date <= #{endDate} " +
            "ORDER BY conversation_date DESC, total_conversations DESC")
    List<Map<String, Object>> getConversationAnalytics(@Param("startDate") String startDate,
                                                       @Param("endDate") String endDate);
}
