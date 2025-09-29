package com.carol.backend.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户注册请求DTO
 */
@Data
public class RegisterRequest {
    
    /**
     * 用户账号 (长度 6-20，字母数字)
     */
    @NotBlank(message = "用户账号不能为空")
    @Size(min = 6, max = 20, message = "用户账号长度必须在6-20位之间")
    @Pattern(regexp = "^[a-zA-Z0-9]{6,20}$", message = "用户账号必须是6-20位字母数字")
    private String userAccount;
    
    /**
     * 密码 (长度 8-20)
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度必须在8-20位之间")
    private String userPassword;
    
    /**
     * 确认密码
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
