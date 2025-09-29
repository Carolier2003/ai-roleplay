package com.carol.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 角色实体类
 * 对应数据库表: characters
 * 
 * @author carol
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("characters")
public class Character {
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBackgroundStory() {
        return backgroundStory;
    }

    public void setBackgroundStory(String backgroundStory) {
        this.backgroundStory = backgroundStory;
    }

    public String getPersonalityTraits() {
        return personalityTraits;
    }

    public void setPersonalityTraits(String personalityTraits) {
        this.personalityTraits = personalityTraits;
    }

    public String getSpeakingStyle() {
        return speakingStyle;
    }

    public void setSpeakingStyle(String speakingStyle) {
        this.speakingStyle = speakingStyle;
    }

    public String getExpertiseArea() {
        return expertiseArea;
    }

    public void setExpertiseArea(String expertiseArea) {
        this.expertiseArea = expertiseArea;
    }

    public String getVoiceStyle() {
        return voiceStyle;
    }

    public void setVoiceStyle(String voiceStyle) {
        this.voiceStyle = voiceStyle;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 角色ID - 主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 角色名称
     */
    @TableField("name")
    private String name;

    /**
     * 头像URL
     */
    @TableField("avatar_url")
    private String avatarUrl;

    /**
     * 角色描述（简短）
     */
    @TableField("description")
    private String description;

    /**
     * 背景故事
     */
    @TableField("background_story")
    private String backgroundStory;

    /**
     * 性格特征
     */
    @TableField("personality_traits")
    private String personalityTraits;

    /**
     * 说话风格
     */
    @TableField("speaking_style")
    private String speakingStyle;

    /**
     * 专业领域
     */
    @TableField("expertise_area")
    private String expertiseArea;

    /**
     * 语音风格
     */
    @TableField("voice_style")
    private String voiceStyle;

    /**
     * 角色状态 (1-启用, 0-禁用)
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
     * 构建角色的完整显示名称
     */
    public String getDisplayName() {
        return name != null ? name : "未知角色";
    }

    /**
     * 检查角色信息是否完整
     */
    public boolean isComplete() {
        return name != null && !name.trim().isEmpty() &&
               backgroundStory != null && !backgroundStory.trim().isEmpty() &&
               personalityTraits != null && !personalityTraits.trim().isEmpty();
    }
}
