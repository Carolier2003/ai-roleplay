package com.carol.backend.dto;

import lombok.Data;

/**
 * 用户响应DTO
 */
@Data
public class UserResponse {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户账号
     */
    private String userAccount;
    
    /**
     * 昵称
     */
    private String displayName;
    
    /**
     * 头像URL
     */
    private String avatarUrl;
    
    /**
     * 用户角色
     */
    private String role;
}
