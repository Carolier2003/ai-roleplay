package com.carol.backend.dto;

import lombok.Data;

/**
 * 登录响应DTO
 */
@Data
public class LoginResponse {
    
    /**
     * 访问令牌
     */
    private String accessToken;
    
    /**
     * 刷新令牌
     */
    private String refreshToken;
    
    /**
     * 用户信息
     */
    private UserInfo user;
    
    @Data
    public static class UserInfo {
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
}
