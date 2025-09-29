package com.carol.backend.service.impl;

import com.carol.backend.dto.profile.ProfileResponse;
import com.carol.backend.dto.profile.UpdateProfileRequest;
import com.carol.backend.entity.User;
import com.carol.backend.enums.ErrorCode;
import com.carol.backend.exception.BusinessException;
import com.carol.backend.mapper.UserMapper;
import com.carol.backend.service.IUserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 用户个人资料服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements IUserProfileService {
    
    private final UserMapper userMapper;
    
    @Override
    public ProfileResponse getUserProfile(Long userId) {
        log.info("[getUserProfile] 获取用户个人资料, userId={}", userId);
        
        User user = userMapper.selectByUserId(userId);
        if (user == null) {
            log.error("[getUserProfile] 用户不存在, userId={}", userId);
            throw BusinessException.of(ErrorCode.USER_NOT_FOUND);
        }
        
        ProfileResponse response = ProfileResponse.builder()
                .userId(user.getUserId())
                .userAccount(user.getUserAccount())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                // 暂时注释掉数据库中不存在的字段
                // .bio(user.getBio())
                // .gender(user.getGender())
                // .birthday(user.getBirthday())
                // .phoneNumber(user.getPhoneNumber())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
        
        log.info("[getUserProfile] 获取用户个人资料成功, userId={}, displayName={}", 
            userId, user.getDisplayName());
        return response;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProfileResponse updateUserProfile(Long userId, UpdateProfileRequest request) {
        log.info("[updateUserProfile] 更新用户个人资料, userId={}, request={}", userId, request);
        
        // 1. 检查用户是否存在
        User user = userMapper.selectByUserId(userId);
        if (user == null) {
            log.error("[updateUserProfile] 用户不存在, userId={}", userId);
            throw BusinessException.of(ErrorCode.USER_NOT_FOUND);
        }
        
        // 2. 检查邮箱是否已被其他用户使用
        if (StringUtils.hasText(request.getEmail()) && 
            !request.getEmail().equals(user.getEmail()) &&
            isEmailTaken(request.getEmail(), userId)) {
            log.warn("[updateUserProfile] 邮箱已被其他用户使用, email={}", request.getEmail());
            throw BusinessException.of(ErrorCode.PROFILE_EMAIL_TAKEN);
        }
        
        // 3. 更新用户信息
        User updateUser = new User();
        updateUser.setUserId(userId);
        updateUser.setDisplayName(request.getDisplayName());
        updateUser.setEmail(StringUtils.hasText(request.getEmail()) ? request.getEmail() : null);
        updateUser.setAvatarUrl(StringUtils.hasText(request.getAvatarUrl()) ? request.getAvatarUrl() : null);
        // 暂时注释掉数据库中不存在的字段
        // updateUser.setBio(StringUtils.hasText(request.getBio()) ? request.getBio() : null);
        // updateUser.setGender(StringUtils.hasText(request.getGender()) ? request.getGender() : null);
        // updateUser.setBirthday(StringUtils.hasText(request.getBirthday()) ? request.getBirthday() : null);
        // updateUser.setPhoneNumber(StringUtils.hasText(request.getPhoneNumber()) ? request.getPhoneNumber() : null);
        updateUser.setUpdatedAt(LocalDateTime.now());
        
        int updateCount = userMapper.updateById(updateUser);
        if (updateCount != 1) {
            log.error("[updateUserProfile] 更新用户信息失败, userId={}, updateCount={}", userId, updateCount);
            throw BusinessException.of(ErrorCode.PROFILE_UPDATE_FAILED);
        }
        
        log.info("[updateUserProfile] 更新用户个人资料成功, userId={}, displayName={}", 
            userId, request.getDisplayName());
        
        // 4. 返回更新后的用户信息
        return getUserProfile(userId);
    }
    
    @Override
    public boolean isEmailTaken(String email, Long excludeUserId) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        
        log.info("[isEmailTaken] 检查邮箱是否被使用, email={}, excludeUserId={}", email, excludeUserId);
        
        User existingUser = userMapper.selectByEmail(email);
        boolean isTaken = existingUser != null && !existingUser.getUserId().equals(excludeUserId);
        
        log.info("[isEmailTaken] 邮箱检查结果, email={}, isTaken={}", email, isTaken);
        return isTaken;
    }
}
