package com.carol.backend.service.impl;

import com.carol.backend.dto.LoginRequest;
import com.carol.backend.dto.LoginResponse;
import com.carol.backend.dto.RegisterRequest;
import com.carol.backend.dto.UserResponse;
import com.carol.backend.entity.User;
import com.carol.backend.enums.ErrorCode;
import com.carol.backend.exception.BusinessException;
import com.carol.backend.service.IAuthService;
import com.carol.backend.service.IUserService;
import com.carol.backend.service.IJwtService;
import com.carol.backend.util.PasswordEncoderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.carol.backend.mapper.UserMapper;
import com.carol.backend.util.UserContext;

import java.util.UUID;

/**
 * 认证服务实现类
 * 
 * @author jianjl
 * @version 1.0
 * @description 用户认证相关的业务逻辑服务实现
 * @date 2025-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {
    
    private final IUserService userService;
    private final IJwtService jwtService;
    private final PasswordEncoderUtil passwordEncoderUtil;
    private final UserMapper userMapper;
    
    @Override
    public UserResponse register(RegisterRequest request) {
        log.info("[register] 开始用户注册: userAccount={}", request.getUserAccount());
        
        try {
            // 检查用户是否已存在
            User existingUser = userService.findByUserAccount(request.getUserAccount());
            if (existingUser != null) {
                log.warn("[register] 用户账号已存在: userAccount={}", request.getUserAccount());
                throw BusinessException.of(ErrorCode.USER_ACCOUNT_EXISTS);
            }
            
            // 创建新用户
            User newUser = new User();
            newUser.setUserAccount(request.getUserAccount());
            newUser.setUserPassword(passwordEncoderUtil.encode(request.getUserPassword()));
            
            // 生成用户名：自动生成 "聊聊+UUID前4位"
            String username = generateUsername(null);
            newUser.setDisplayName(username);
            
            newUser.setStatus(1); // 正常状态
            newUser.setLoginCount(0); // 初始登录次数为0
            
            // 保存用户
            User savedUser = userService.save(newUser);
            
            log.info("[register] 用户注册成功: userId={}, userAccount={}", savedUser.getUserId(), savedUser.getUserAccount());
            
            // 返回用户信息
            UserResponse response = new UserResponse();
            response.setUserId(savedUser.getUserId());
            response.setUserAccount(savedUser.getUserAccount());
            response.setDisplayName(savedUser.getDisplayName());
            response.setAvatarUrl(savedUser.getAvatarUrl());
            response.setRole(savedUser.getRole());
            
            return response;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[register] 用户注册失败: userAccount={}, error={}", request.getUserAccount(), e.getMessage(), e);
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "用户注册失败", e);
        }
    }
    
    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("[login] 开始用户登录: userAccount={}", request.getUserAccount());
        
        try {
            // 1. 根据用户账号查询用户
            User user = userService.findByUserAccount(request.getUserAccount());
            
            // 2. 检查用户是否存在
            if (user == null) {
                log.warn("[login] 账号不存在: userAccount={}", request.getUserAccount());
                throw BusinessException.of(ErrorCode.USER_NOT_FOUND, "账号不存在");
            }
            
            // 3. 校验密码
            if (!passwordEncoderUtil.matches(request.getUserPassword(), user.getUserPassword())) {
                log.warn("[login] 密码错误: userAccount={}", request.getUserAccount());
                throw BusinessException.of(ErrorCode.USER_PASSWORD_ERROR);
            }
            
            // 4. 生成令牌
            String accessToken = jwtService.generateAccessToken(user.getUserAccount(), user.getUserId());
            String refreshToken = jwtService.generateRefreshToken(user.getUserAccount(), user.getUserId());
            
            // 5. 更新最后登录时间
            userService.updateLastLogin(user.getUserId());
            
            // 6. 构建响应
            LoginResponse response = new LoginResponse();
            response.setAccessToken(accessToken);
            response.setRefreshToken(refreshToken);
            
            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
            userInfo.setUserId(user.getUserId());
            userInfo.setUserAccount(user.getUserAccount());
            userInfo.setDisplayName(user.getDisplayName());
            userInfo.setAvatarUrl(user.getAvatarUrl());
            userInfo.setRole(user.getRole());
            response.setUser(userInfo);
            
            log.info("[login] 用户登录成功: userId={}, userAccount={}", user.getUserId(), user.getUserAccount());
            return response;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[login] 用户登录失败: userAccount={}, error={}", request.getUserAccount(), e.getMessage(), e);
            throw BusinessException.of(ErrorCode.AUTH_FAILED, "登录失败", e);
        }
    }
    
    @Override
    public LoginResponse refreshToken(String refreshToken) {
        log.info("[refreshToken] 开始刷新令牌");
        
        try {
            // 1. 验证刷新令牌
            if (!jwtService.validateToken(refreshToken)) {
                log.warn("[refreshToken] 无效的刷新令牌");
                throw BusinessException.of(ErrorCode.TOKEN_INVALID, "无效的刷新令牌");
            }
            
            String userAccount = jwtService.getUserAccountFromToken(refreshToken);
            String tokenType = jwtService.getTokenTypeFromToken(refreshToken);
            
            if (!"refresh".equals(tokenType)) {
                log.warn("[refreshToken] 无效的刷新令牌类型: tokenType={}", tokenType);
                throw BusinessException.of(ErrorCode.TOKEN_INVALID, "无效的刷新令牌类型");
            }
            
            // 2. 查找用户
            User user = userService.findByUserAccount(userAccount);
            if (user == null) {
                log.warn("[refreshToken] 用户不存在: userAccount={}", userAccount);
                throw BusinessException.of(ErrorCode.USER_NOT_FOUND);
            }
            
            // 3. 生成新的访问令牌
            String newAccessToken = jwtService.generateAccessToken(user.getUserAccount(), user.getUserId());
            String newRefreshToken = jwtService.generateRefreshToken(user.getUserAccount(), user.getUserId());
            
            // 4. 构建响应
            LoginResponse response = new LoginResponse();
            response.setAccessToken(newAccessToken);
            response.setRefreshToken(newRefreshToken);
            
            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
            userInfo.setUserId(user.getUserId());
            userInfo.setUserAccount(user.getUserAccount());
            userInfo.setDisplayName(user.getDisplayName());
            userInfo.setAvatarUrl(user.getAvatarUrl());
            userInfo.setRole(user.getRole());
            response.setUser(userInfo);
            
            log.info("[refreshToken] 令牌刷新成功: userId={}, userAccount={}", user.getUserId(), userAccount);
            return response;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[refreshToken] 刷新令牌失败: error={}", e.getMessage(), e);
            throw BusinessException.of(ErrorCode.TOKEN_INVALID, "刷新令牌无效", e);
        }
    }
    
    @Override
    public UserResponse getCurrentUser() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw BusinessException.of(ErrorCode.LOGIN_REQUIRED);
        }
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of(ErrorCode.USER_NOT_FOUND);
        }
        
        UserResponse response = new UserResponse();
        response.setUserId(user.getUserId());
        response.setUserAccount(user.getUserAccount());
        response.setDisplayName(user.getDisplayName());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setRole(user.getRole());
        
        return response;
    }

    /**
     * 生成用户名
     * 如果用户没有提供用户名，则自动生成 "聊聊+UUID前4位"
     */
    private String generateUsername(String providedUsername) {
        if (providedUsername != null && !providedUsername.trim().isEmpty()) {
            log.debug("[generateUsername] 使用用户提供的用户名: {}", providedUsername);
            return providedUsername.trim();
        }
        
        // 生成随机用户名：聊聊 + UUID前4位
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String randomSuffix = uuid.substring(0, 4).toUpperCase();
        String generatedUsername = "聊聊" + randomSuffix;
        
        log.debug("[generateUsername] 自动生成用户名: {}", generatedUsername);
        return generatedUsername;
    }
}
