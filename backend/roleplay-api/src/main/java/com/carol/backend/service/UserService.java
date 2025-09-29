package com.carol.backend.service;

import com.carol.backend.dto.RegisterRequest;
import com.carol.backend.dto.UserResponse;
import com.carol.backend.entity.User;
import com.carol.backend.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import com.carol.backend.util.PasswordEncoderUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 用户服务
 */
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserMapper userMapper;
    private final PasswordEncoderUtil passwordEncoderUtil;
    
    // 用户账号格式验证：8-12位数字
    private static final Pattern USER_ACCOUNT_PATTERN = Pattern.compile("^\\d{8,12}$");
    
    // 密码格式验证：6-12位，包含大小写字母和特殊字符
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{6,12}$");
    
    /**
     * 用户注册
     */
    public UserResponse register(RegisterRequest request) {
        // 1. 校验用户账号格式
        if (!USER_ACCOUNT_PATTERN.matcher(request.getUserAccount()).matches()) {
            throw new IllegalArgumentException("用户账号格式不正确，必须是8-12位数字");
        }
        
        // 2. 校验密码确认
        if (!request.getUserPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }
        
        // 4. 检查用户账号是否已存在
        if (userMapper.countByUserAccount(request.getUserAccount()) > 0) {
            throw new IllegalArgumentException("账号已存在");
        }
        
        // 3. 创建用户
        User user = new User();
        user.setUserAccount(request.getUserAccount());
        user.setUserPassword(passwordEncoderUtil.encode(request.getUserPassword()));
        user.setDisplayName(request.getUserAccount()); // 默认使用账号作为昵称
        user.setStatus(1); // 正常状态
        user.setLoginCount(0); // 初始登录次数为0
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        // 6. 保存用户
        userMapper.insert(user);
        
        // 7. 返回用户信息
        UserResponse response = new UserResponse();
        response.setUserId(user.getUserId());
        response.setUserAccount(user.getUserAccount());
        response.setDisplayName(user.getDisplayName());
        response.setAvatarUrl(user.getAvatarUrl());
        
        return response;
    }
    
    /**
     * 根据用户账号查找用户
     */
    public User findByUserAccount(String userAccount) {
        // 尝试按用户账号查找
        return  userMapper.findByUserAccount(userAccount);
    }
    
    /**
     * 保存用户（简化版本）
     */
    public User save(User user) {
        if (user.getUserId() == null) {
            // 新增用户
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.insert(user);
        } else {
            // 更新用户
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
        }
        return user;
    }
    
    /**
     * 更新用户最后登录时间和登录次数
     */
    public void updateLastLogin(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setLastLoginAt(LocalDateTime.now());
            user.setLoginCount((user.getLoginCount() != null ? user.getLoginCount() : 0) + 1);
            userMapper.updateById(user);
        }
    }
}
