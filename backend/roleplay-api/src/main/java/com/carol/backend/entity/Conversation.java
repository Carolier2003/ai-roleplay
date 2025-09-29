package com.carol.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话会话实体（数据仓库版本）
 * 用于存储从Redis同步的对话历史数据
 * 
 * @author carol
 */
@Data
@Accessors(chain = true)
@TableName("conversations")
public class Conversation {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 会话ID（对应Redis的conversationId）
     */
    @TableField("session_id")
    private String sessionId;
    
    /**
     * 角色ID
     */
    @TableField("character_id")
    private Long characterId;
    
    /**
     * 用户ID
     */
    @TableField("user_id")
    private String userId;
    
    /**
     * 对话标题（自动生成或用户设置）
     */
    @TableField("title")
    private String title;
    
    /**
     * 对话内容摘要
     */
    @TableField("context_summary")
    private String contextSummary;
    
    /**
     * 消息总数
     */
    @TableField("message_count")
    private Integer messageCount;
    
    /**
     * 总Token消耗
     */
    @TableField("total_tokens")
    private Integer totalTokens;
    
    /**
     * 对话开始时间
     */
    @TableField("start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    
    /**
     * 对话结束时间
     */
    @TableField("end_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    
    /**
     * 对话持续时间（分钟）
     */
    @TableField("duration_minutes")
    private Integer durationMinutes;
    
    /**
     * 同步来源: redis, manual
     */
    @TableField("sync_source")
    private String syncSource;
    
    /**
     * 同步状态: 1-已同步 2-同步失败
     */
    @TableField("sync_status")
    private Integer syncStatus;
    
    /**
     * 对话质量评分（1-5）
     */
    @TableField("quality_score")
    private BigDecimal qualityScore;
    
    /**
     * 用户评分: 1-5
     */
    @TableField("feedback_rating")
    private Integer feedbackRating;
    
    /**
     * 对话标签
     */
    @TableField("tags")
    private String tags;
    
    /**
     * 导出次数
     */
    @TableField("export_count")
    private Integer exportCount;
    
    /**
     * 最后同步时间
     */
    @TableField("last_sync_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastSyncAt;
    
    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    // 非数据库字段
    /**
     * 角色名称（关联查询）
     */
    @TableField(exist = false)
    private String characterName;
    
    /**
     * 对话消息列表
     */
    @TableField(exist = false)
    private List<ConversationMessage> messages;
}
