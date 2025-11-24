package com.carol.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库表：users
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("users")
public class User {
    
    /**
     * 用户ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long userId;
    
    /**
     * 用户账号（登录名）
     */
    @TableField("user_account")
    private String userAccount;
    
    /**
     * 用户密码（加密存储）
     */
    @TableField("user_password")
    private String userPassword;
    
    /**
     * 用户昵称/显示名
     */
    @TableField("username")
    private String displayName;
    
    /**
     * 邮箱地址
     */
    @TableField("email")
    private String email;
    
    /**
     * 头像URL
     */
    @TableField("avatar_url")
    private String avatarUrl;
    
    /**
     * 状态: 1-正常 0-禁用
     */
    @TableField("status")
    private Integer status;
    
    /**
     * 最后登录时间
     */
    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;
    
    /**
     * 登录次数
     */
    @TableField("login_count")
    private Integer loginCount;
    
    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 用户角色 (USER, ADMIN)
     */
    @TableField("role")
    private String role;
    
    // 注释掉数据库中不存在的字段，避免查询错误
    // 如果需要这些字段，请先在数据库中添加对应的列
    
    // /**
    //  * 个人简介
    //  */
    // @TableField("bio")
    // private String bio;
    
    // /**
    //  * 性别 (M: 男性, F: 女性, U: 未知)
    //  */
    // @TableField("gender")
    // private String gender;
    
    // /**
    //  * 生日 (格式: YYYY-MM-DD)
    //  */
    // @TableField("birthday")
    // private String birthday;
    
    // /**
    //  * 手机号码
    //  */
    // @TableField("phone_number")
    // private String phoneNumber;
}
