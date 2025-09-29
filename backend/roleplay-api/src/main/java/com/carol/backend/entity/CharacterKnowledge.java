package com.carol.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色知识实体类
 * 用于存储角色相关的知识信息，支持RAG检索
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("character_knowledge")
public class CharacterKnowledge {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 角色ID
     */
    @TableField("character_id")
    private Long characterId;

    /**
     * 知识标题
     */
    @TableField("title")
    private String title;

    /**
     * 知识内容
     */
    @TableField("content")
    private String content;

    /**
     * 知识类型
     * PERSONALITY - 性格特征
     * BASIC_INFO - 基本信息
     * KNOWLEDGE - 专业知识
     * EVENTS - 重要事件
     * RELATIONSHIPS - 人际关系
     * ABILITIES - 能力技能
     * QUOTES - 经典语录
     */
    @TableField("knowledge_type")
    private String knowledgeType;

    /**
     * 重要性评分 (1-10)
     */
    @TableField("importance_score")
    private Integer importanceScore;

    /**
     * 数据来源
     */
    @TableField("source")
    private String source;

    /**
     * 原始链接
     */
    @TableField("source_url")
    private String sourceUrl;

    /**
     * 向量数据库ID
     */
    @TableField("vector_id")
    private String vectorId;

    /**
     * 标签列表 (JSON格式)
     */
    @TableField("tags")
    private String tags;

    /**
     * 语言
     */
    @TableField("language")
    private String language;

    /**
     * 状态 (1-启用, 0-禁用)
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标志
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    // 辅助方法

    /**
     * 获取标签列表
     */
    public List<String> getTagList() {
        if (tags == null || tags.trim().isEmpty()) {
            return List.of();
        }
        try {
            // 简单的JSON数组解析
            return List.of(tags.replace("[", "").replace("]", "").replace("\"", "").split(","));
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 设置标签列表
     */
    public void setTagList(List<String> tagList) {
        if (tagList == null || tagList.isEmpty()) {
            this.tags = null;
        } else {
            this.tags = "[\"" + String.join("\",\"", tagList) + "\"]";
        }
    }

    /**
     * 是否为有效知识
     */
    public boolean isValid() {
        return title != null && !title.trim().isEmpty() 
            && content != null && !content.trim().isEmpty()
            && content.length() > 10;
    }

    /**
     * 获取显示标题
     */
    public String getDisplayTitle() {
        if (title != null && title.length() > 50) {
            return title.substring(0, 50) + "...";
        }
        return title;
    }

    /**
     * 获取内容摘要
     */
    public String getContentSummary() {
        if (content != null && content.length() > 100) {
            return content.substring(0, 100) + "...";
        }
        return content;
    }
}
