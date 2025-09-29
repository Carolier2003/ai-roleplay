package com.carol.backend.controller;

import com.carol.backend.dto.profile.ProfileResponse;
import com.carol.backend.dto.profile.UpdateProfileRequest;
import com.carol.backend.enums.ErrorCode;
import com.carol.backend.exception.BusinessException;
import com.carol.backend.service.IUserProfileService;
import com.carol.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 用户个人资料控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Validated
public class UserProfileController {
    
    private final IUserProfileService userProfileService;
    
    /**
     * 获取当前用户个人资料
     */
    @GetMapping
    public ProfileResponse getCurrentUserProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("[getCurrentUserProfile] 获取当前用户个人资料, userId={}", userId);
        
        ProfileResponse profile = userProfileService.getUserProfile(userId);
        
        log.info("[getCurrentUserProfile] 获取个人资料成功, userId={}, displayName={}", 
            userId, profile.getDisplayName());
        return profile;
    }
    
    /**
     * 获取指定用户个人资料（公开信息）
     */
    @GetMapping("/{userId}")
    public ProfileResponse getUserProfile(@PathVariable Long userId) {
        log.info("[getUserProfile] 获取用户个人资料, userId={}", userId);
        
        ProfileResponse profile = userProfileService.getUserProfile(userId);
        
        // 对于非当前用户，隐藏敏感信息
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!userId.equals(currentUserId)) {
            profile.setEmail(null);
            // 暂时注释掉数据库中不存在的字段
            // profile.setPhoneNumber(null);
        }
        
        log.info("[getUserProfile] 获取用户个人资料成功, userId={}, displayName={}", 
            userId, profile.getDisplayName());
        return profile;
    }
    
    /**
     * 更新当前用户个人资料
     */
    @PutMapping
    public ProfileResponse updateCurrentUserProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("[updateCurrentUserProfile] 更新当前用户个人资料, userId={}, request={}", userId, request);
        
        ProfileResponse updatedProfile = userProfileService.updateUserProfile(userId, request);
        
        log.info("[updateCurrentUserProfile] 更新个人资料成功, userId={}, displayName={}", 
            userId, updatedProfile.getDisplayName());
        return updatedProfile;
    }
    
    /**
     * 检查邮箱是否可用
     */
    @GetMapping("/check-email")
    public Boolean checkEmailAvailability(@RequestParam String email) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("[checkEmailAvailability] 检查邮箱可用性, email={}, currentUserId={}", email, currentUserId);
        
        boolean isAvailable = !userProfileService.isEmailTaken(email, currentUserId);
        
        log.info("[checkEmailAvailability] 邮箱可用性检查完成, email={}, isAvailable={}", email, isAvailable);
        return isAvailable;
    }
}
