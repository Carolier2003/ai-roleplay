package com.carol.backend.service;

import com.carol.backend.dto.avatar.AvatarUploadResponse;
import com.carol.backend.dto.avatar.CropData;
import org.springframework.web.multipart.MultipartFile;

/**
 * 头像管理服务接口
 */
public interface IAvatarService {
    
    /**
     * 上传并处理头像
     * 
     * @param userId 用户ID
     * @param file 头像文件
     * @param cropData 裁剪参数（可选）
     * @return 头像上传响应
     */
    AvatarUploadResponse uploadAvatar(Long userId, MultipartFile file, CropData cropData);
    
    /**
     * 删除用户头像
     * 
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteAvatar(Long userId);
    
    /**
     * 获取用户头像URL
     * 
     * @param userId 用户ID
     * @return 头像URL
     */
    String getAvatarUrl(Long userId);
}
