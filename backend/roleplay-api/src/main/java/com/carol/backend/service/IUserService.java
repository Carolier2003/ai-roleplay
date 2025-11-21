package com.carol.backend.service;

import com.carol.backend.dto.RegisterRequest;
import com.carol.backend.dto.UserResponse;
import com.carol.backend.entity.User;

/**
 * 用户服务接口
 * 
 * @author jianjl
 * @version 1.0
 * @description 用户相关的业务逻辑服务
 * @date 2025-01-15
 */
public interface IUserService {
    
    /**
     * 用户注册
     * 
     * @param request 注册请求
     * @return 用户响应
     */
    UserResponse register(RegisterRequest request);
    
    /**
     * 根据用户账号查找用户
     * 
     * @param userAccount 用户账号
     * @return 用户实体
     */
    User findByUserAccount(String userAccount);
    
    /**
     * 保存用户
     * 
     * @param user 用户实体
     * @return 保存后的用户实体
     */
    User save(User user);
    
    /**
     * 更新用户最后登录时间和登录次数
     * 
     * @param userId 用户ID
     */
    void updateLastLogin(Long userId);
}
