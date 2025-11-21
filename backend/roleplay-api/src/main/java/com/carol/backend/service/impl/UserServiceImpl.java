package com.carol.backend.service.impl;

import com.carol.backend.dto.RegisterRequest;
import com.carol.backend.dto.UserResponse;
import com.carol.backend.entity.User;
import com.carol.backend.enums.ErrorCode;
import com.carol.backend.exception.BusinessException;
import com.carol.backend.mapper.UserMapper;
import com.carol.backend.service.IUserService;
import com.carol.backend.util.PasswordEncoderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 用户服务实现类
 * 
 * @author jianjl
 * @version 1.0
 * @description 用户相关的业务逻辑服务实现
 * @date 2025-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {
    
    private final UserMapper userMapper;
    private final PasswordEncoderUtil passwordEncoderUtil;
    
    // 用户账号格式验证：8-12位数字
    private static final Pattern USER_ACCOUNT_PATTERN = Pattern.compile("^\\d{8,12}$");
    
    // 密码格式验证：6-12位，包含大小写字母和特殊字符
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{6,12}$");
    
    @Override
    public UserResponse register(RegisterRequest request) {
        log.info("[register] 开始用户注册: userAccount={}", request.getUserAccount());
        
        try {
            // 1. 校验用户账号格式
            if (!USER_ACCOUNT_PATTERN.matcher(request.getUserAccount()).matches()) {
                log.warn("[register] 用户账号格式不正确: userAccount={}", request.getUserAccount());
                throw BusinessException.of(ErrorCode.PARAM_ERROR, "用户账号格式不正确，必须是8-12位数字");
            }
            
            // 2. 校验密码确认
            if (!request.getUserPassword().equals(request.getConfirmPassword())) {
                log.warn("[register] 两次输入的密码不一致: userAccount={}", request.getUserAccount());
                throw BusinessException.of(ErrorCode.PARAM_ERROR, "两次输入的密码不一致");
            }
            
            // 3. 检查用户账号是否已存在
            if (userMapper.countByUserAccount(request.getUserAccount()) > 0) {
                log.warn("[register] 用户账号已存在: userAccount={}", request.getUserAccount());
                throw BusinessException.of(ErrorCode.USER_ACCOUNT_EXISTS);
            }
            
            // 4. 创建用户
            User user = new User();
            user.setUserAccount(request.getUserAccount());
            user.setUserPassword(passwordEncoderUtil.encode(request.getUserPassword()));
            user.setDisplayName(request.getUserAccount()); // 默认使用账号作为昵称
            user.setStatus(1); // 正常状态
            user.setLoginCount(0); // 初始登录次数为0
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            
            // 5. 保存用户
            userMapper.insert(user);
            
            // 6. 返回用户信息
            UserResponse response = new UserResponse();
            response.setUserId(user.getUserId());
            response.setUserAccount(user.getUserAccount());
            response.setDisplayName(user.getDisplayName());
            response.setAvatarUrl(user.getAvatarUrl());
            
            log.info("[register] 用户注册成功: userId={}, userAccount={}", user.getUserId(), user.getUserAccount());
            return response;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[register] 用户注册失败: userAccount={}, error={}", request.getUserAccount(), e.getMessage(), e);
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "用户注册失败", e);
        }
    }
    
    @Override
    public User findByUserAccount(String userAccount) {
        log.debug("[findByUserAccount] 查找用户: userAccount={}", userAccount);
        
        try {
            User user = userMapper.findByUserAccount(userAccount);
            log.debug("[findByUserAccount] 查找结果: userAccount={}, found={}", userAccount, user != null);
            return user;
        } catch (Exception e) {
            log.error("[findByUserAccount] 查找用户失败: userAccount={}, error={}", userAccount, e.getMessage(), e);
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "查找用户失败", e);
        }
    }
    
    @Override
    public User save(User user) {
        log.debug("[save] 保存用户: userId={}", user.getUserId());
        
        try {
            if (user.getUserId() == null) {
                // 新增用户
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                userMapper.insert(user);
                log.info("[save] 新增用户成功: userId={}", user.getUserId());
            } else {
                // 更新用户
                user.setUpdatedAt(LocalDateTime.now());
                userMapper.updateById(user);
                log.info("[save] 更新用户成功: userId={}", user.getUserId());
            }
            return user;
        } catch (Exception e) {
            log.error("[save] 保存用户失败: userId={}, error={}", user.getUserId(), e.getMessage(), e);
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "保存用户失败", e);
        }
    }
    
    @Override
    public void updateLastLogin(Long userId) {
        log.debug("[updateLastLogin] 更新最后登录时间: userId={}", userId);
        
        try {
            User user = userMapper.selectById(userId);
            if (user != null) {
                user.setLastLoginAt(LocalDateTime.now());
                user.setLoginCount((user.getLoginCount() != null ? user.getLoginCount() : 0) + 1);
                userMapper.updateById(user);
                log.info("[updateLastLogin] 更新成功: userId={}, loginCount={}", userId, user.getLoginCount());
            } else {
                log.warn("[updateLastLogin] 用户不存在: userId={}", userId);
            }
        } catch (Exception e) {
            log.error("[updateLastLogin] 更新最后登录时间失败: userId={}, error={}", userId, e.getMessage(), e);
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "更新最后登录时间失败", e);
        }
    }
}
