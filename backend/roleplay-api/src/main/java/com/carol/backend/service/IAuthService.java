package com.carol.backend.service;

import com.carol.backend.dto.LoginRequest;
import com.carol.backend.dto.LoginResponse;
import com.carol.backend.dto.RegisterRequest;
import com.carol.backend.dto.UserResponse;

/**
 * 认证服务接口
 * 
 * @author jianjl
 * @version 1.0
 * @description 用户认证相关的业务逻辑服务
 * @date 2025-01-15
 */
public interface IAuthService {
    
    /**
     * 用户注册
     * 
     * @param request 注册请求
     * @return 用户响应
     */
    UserResponse register(RegisterRequest request);
    
    /**
     * 用户登录
     * 
     * @param request 登录请求
     * @return 登录响应
     */
    LoginResponse login(LoginRequest request);
    
    /**
     * 刷新令牌
     * 
     * @param refreshToken 刷新令牌
     * @return 登录响应
     */
    LoginResponse refreshToken(String refreshToken);
}
