package com.carol.backend.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户登录请求DTO
 */
@Data
public class LoginRequest {
    
    /**
     * 用户账号
     */
    @NotBlank(message = "用户账号不能为空")
    private String userAccount;
    
    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String userPassword;
}
