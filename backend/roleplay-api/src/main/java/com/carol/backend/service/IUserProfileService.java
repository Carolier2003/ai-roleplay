package com.carol.backend.service;

import com.carol.backend.dto.profile.ProfileResponse;
import com.carol.backend.dto.profile.UpdateProfileRequest;

/**
 * 用户个人资料服务接口
 */
public interface IUserProfileService {
    
    /**
     * 获取用户个人资料
     * 
     * @param userId 用户ID
     * @return 个人资料信息
     */
    ProfileResponse getUserProfile(Long userId);
    
    /**
     * 更新用户个人资料
     * 
     * @param userId 用户ID
     * @param request 更新请求
     * @return 更新后的个人资料信息
     */
    ProfileResponse updateUserProfile(Long userId, UpdateProfileRequest request);
    
    /**
     * 检查邮箱是否已被其他用户使用
     * 
     * @param email 邮箱地址
     * @param excludeUserId 排除的用户ID（当前用户）
     * @return true: 已被使用, false: 未被使用
     */
    boolean isEmailTaken(String email, Long excludeUserId);
}
