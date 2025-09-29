package com.carol.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 对话消息实体（数据仓库版本）
 * 用于存储从Redis同步的对话消息历史数据
 * 
 * @author carol
 */
@Data
@Accessors(chain = true)
@TableName("conversation_messages")
public class ConversationMessage {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 会话ID
     */
    @TableField("conversation_id")
    private Long conversationId;
    
    /**
     * 会话标识
     */
    @TableField("session_id")
    private String sessionId;
    
    /**
     * 消息在会话中的序号
     */
    @TableField("message_index")
    private Integer messageIndex;
    
    /**
     * 消息类型: 1-用户 2-AI 3-系统
     */
    @TableField("message_type")
    private Integer messageType;
    
    /**
     * 消息内容
     */
    @TableField("content")
    private String content;
    
    /**
     * 内容字符数
     */
    @TableField("content_length")
    private Integer contentLength;
    
    /**
     * 语音文件URL
     */
    @TableField("audio_url")
    private String audioUrl;
    
    /**
     * 语音时长（秒）
     */
    @TableField("voice_duration")
    private Integer voiceDuration;
    
    /**
     * AI响应时间（毫秒）
     */
    @TableField("response_time_ms")
    private Integer responseTimeMs;
    
    /**
     * Token消耗数量
     */
    @TableField("token_count")
    private Integer tokenCount;
    
    /**
     * 使用的模型名称
     */
    @TableField("model_name")
    private String modelName;
    
    /**
     * 模型温度参数
     */
    @TableField("temperature")
    private BigDecimal temperature;
    
    /**
     * 是否使用了RAG知识
     */
    @TableField("rag_knowledge_used")
    private Boolean ragKnowledgeUsed;
    
    /**
     * 使用的RAG知识条目数
     */
    @TableField("rag_knowledge_count")
    private Integer ragKnowledgeCount;
    
    /**
     * 情感分析得分（-1到1）
     */
    @TableField("sentiment_score")
    private BigDecimal sentimentScore;
    
    /**
     * 消息语言
     */
    @TableField("language")
    private String language;
    
    /**
     * 消息元数据
     */
    @TableField("metadata")
    private String metadata;
    
    /**
     * 同步来源
     */
    @TableField("sync_source")
    private String syncSource;
    
    /**
     * 消息原始时间戳
     */
    @TableField("message_timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime messageTimestamp;
    
    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    /**
     * 消息类型枚举
     */
    public enum MessageType {
        USER(1, "用户"),
        AI(2, "AI"),
        SYSTEM(3, "系统");
        
        private final int code;
        private final String description;
        
        MessageType(int code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public int getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static MessageType valueOf(int code) {
            for (MessageType type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown message type code: " + code);
        }
    }
}
