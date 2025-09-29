package com.carol.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 聊天消息视图对象
 * 用于前端展示聊天历史记录
 * 
 * @author carol
 */
@Data
@Accessors(chain = true)
public class ConversationMessageVO {
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 角色ID
     */
    private Long characterId;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息类型: 1-用户 2-AI 3-系统
     */
    private Integer messageType;
    
    /**
     * 是否为用户消息
     */
    private Boolean isUser;
    
    /**
     * 消息时间戳
     */
    private Long timestamp;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
    
    /**
     * 语音文件URL
     */
    private String audioUrl;
    
    /**
     * 语音时长（秒）
     */
    private Integer voiceDuration;
    
    /**
     * 数据来源标识: redis/mysql/redis_updated
     */
    private String dataSource;
    
    /**
     * 是否需要同步到MySQL
     */
    private Boolean needSync;
}
