package com.carol.backend.dto.profile;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 个人资料响应DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProfileResponse {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户账号
     */
    private String userAccount;
    
    /**
     * 显示名称
     */
    private String displayName;
    
    /**
     * 邮箱地址
     */
    private String email;
    
    /**
     * 头像URL
     */
    private String avatarUrl;
    
    // 暂时注释掉数据库中不存在的字段
    // /**
    //  * 个人简介
    //  */
    // private String bio;
    
    // /**
    //  * 性别 (M: 男性, F: 女性, U: 未知)
    //  */
    // private String gender;
    
    // /**
    //  * 生日 (格式: YYYY-MM-DD)
    //  */
    // private String birthday;
    
    // /**
    //  * 手机号码
    //  */
    // private String phoneNumber;
    
    /**
     * 账号创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 最后更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginAt;
}
