package com.carol.backend.service;

import com.carol.backend.dto.LoginRequest;
import com.carol.backend.dto.LoginResponse;
import com.carol.backend.dto.RegisterRequest;
import com.carol.backend.dto.UserResponse;
import com.carol.backend.entity.User;
import com.carol.backend.util.JwtUtil;
import com.carol.backend.util.PasswordEncoderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.UUID;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoderUtil passwordEncoderUtil;
    
    /**
     * 用户注册
     */
    public UserResponse register(RegisterRequest request) {
        log.info("[register] 开始用户注册, userAccount={}", request.getUserAccount());
        
        // 检查用户是否已存在
        User existingUser = userService.findByUserAccount(request.getUserAccount());
        if (existingUser != null) {
            log.warn("[register] 用户账号已存在, userAccount={}", request.getUserAccount());
            throw new IllegalArgumentException("用户账号已存在");
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
        
        log.info("[register] 用户注册成功, userId={}, userAccount={}", savedUser.getUserId(), savedUser.getUserAccount());
        
        // 返回用户信息
        UserResponse response = new UserResponse();
        response.setUserId(savedUser.getUserId());
        response.setUserAccount(savedUser.getUserAccount());
        response.setDisplayName(savedUser.getDisplayName());
        response.setAvatarUrl(savedUser.getAvatarUrl());
        
        return response;
    }
    
    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest request) {
        // 1. 根据用户账号查询用户
        User user = userService.findByUserAccount(request.getUserAccount());
        
        // 2. 检查用户是否存在
        if (user == null) {
            throw new IllegalArgumentException("账号不存在");
        }
        
        // 3. 校验密码
        if (!passwordEncoderUtil.matches(request.getUserPassword(), user.getUserPassword())) {
            throw new IllegalArgumentException("密码错误");
        }
        
        // 4. 生成令牌
        String accessToken = jwtUtil.generateAccessToken(user.getUserAccount(), user.getUserId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserAccount(), user.getUserId());
        
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
        response.setUser(userInfo);
        
        return response;
    }
    
    /**
     * 刷新令牌
     */
    public LoginResponse refreshToken(String refreshToken) {
        try {
            // 1. 验证刷新令牌
            if (!jwtUtil.validateToken(refreshToken)) {
                throw new IllegalArgumentException("无效的刷新令牌");
            }
            
            String userAccount = jwtUtil.getUserAccountFromToken(refreshToken);
            String tokenType = jwtUtil.getTokenTypeFromToken(refreshToken);
            
            if (!"refresh".equals(tokenType)) {
                throw new IllegalArgumentException("无效的刷新令牌类型");
            }
            
            // 2. 查找用户
            User user = userService.findByUserAccount(userAccount);
            if (user == null) {
                throw new IllegalArgumentException("用户不存在");
            }
            
            // 3. 生成新的访问令牌
            String newAccessToken = jwtUtil.generateAccessToken(user.getUserAccount(), user.getUserId());
            String newRefreshToken = jwtUtil.generateRefreshToken(user.getUserAccount(), user.getUserId());
            
            // 4. 构建响应
            LoginResponse response = new LoginResponse();
            response.setAccessToken(newAccessToken);
            response.setRefreshToken(newRefreshToken);
            
            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
            userInfo.setUserId(user.getUserId());
            userInfo.setUserAccount(user.getUserAccount());
            userInfo.setDisplayName(user.getDisplayName());
            userInfo.setAvatarUrl(user.getAvatarUrl());
            response.setUser(userInfo);
            
            return response;
        } catch (Exception e) {
            throw new IllegalArgumentException("刷新令牌无效");
        }
    }
    
    /**
     * 生成用户名
     * 如果用户没有提供用户名，则自动生成 "聊聊+UUID前4位"
     */
    private String generateUsername(String providedUsername) {
        if (providedUsername != null && !providedUsername.trim().isEmpty()) {
            log.info("[generateUsername] 使用用户提供的用户名: {}", providedUsername);
            return providedUsername.trim();
        }
        
        // 生成随机用户名：聊聊 + UUID前4位
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String randomSuffix = uuid.substring(0, 4).toUpperCase();
        String generatedUsername = "聊聊" + randomSuffix;
        
        log.info("[generateUsername] 自动生成用户名: {}", generatedUsername);
        return generatedUsername;
    }
}
