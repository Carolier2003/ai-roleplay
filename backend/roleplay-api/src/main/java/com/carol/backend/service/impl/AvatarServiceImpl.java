package com.carol.backend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.carol.backend.config.OssConfig;
import com.carol.backend.dto.avatar.AvatarUploadResponse;
import com.carol.backend.dto.avatar.CropData;
import com.carol.backend.entity.User;
import com.carol.backend.enums.ErrorCode;
import com.carol.backend.exception.BusinessException;
import com.carol.backend.mapper.UserMapper;
import com.carol.backend.service.IAvatarService;
import com.carol.backend.service.IImageProcessService;
import com.carol.backend.service.IOssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * 头像管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AvatarServiceImpl implements IAvatarService {
    
    private final IOssService ossService;
    private final IImageProcessService imageProcessService;
    private final UserMapper userMapper;
    private final OssConfig ossConfig;
    
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/webp"
    );
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AvatarUploadResponse uploadAvatar(Long userId, MultipartFile file, CropData cropData) {
        log.info("[uploadAvatar] 开始上传头像, userId={}, fileName={}, size={} bytes", 
            userId, file.getOriginalFilename(), file.getSize());
        
        // 1. 验证用户存在
        User user = userMapper.selectByUserId(userId);
        if (user == null) {
            log.error("[uploadAvatar] 用户不存在, userId={}", userId);
            throw BusinessException.of(ErrorCode.USER_NOT_FOUND);
        }
        
        // 2. 验证文件
        validateFile(file);
        
        try {
            // 3. 读取文件内容
            byte[] originalBytes;
            try {
                originalBytes = file.getBytes();
            } catch (IOException e) {
                log.error("[uploadAvatar] 读取文件内容失败, userId={}: {}", userId, e.getMessage(), e);
                throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "读取文件内容失败");
            }
            
            // 4. 验证图片格式
            String format = imageProcessService.validateImageFormat(originalBytes);
            
            // 5. 获取图片尺寸
            int[] dimensions = imageProcessService.getImageDimensions(originalBytes);
            
            // 6. 删除旧头像
            deleteOldAvatar(user);
            
            // 7. 生成文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filePrefix = "user" + userId + "_" + timestamp;
            
            // 8. 处理图片
            byte[] processedBytes = originalBytes;
            
            // 如果有裁剪参数，进行裁剪
            if (cropData != null) {
                processedBytes = imageProcessService.cropImage(originalBytes, cropData);
            }
            
            // 压缩处理后的图片
            processedBytes = imageProcessService.compressImage(
                processedBytes, 
                ossConfig.getAvatar().getProcessedSize(), 
                0.9f
            );
            
            // 生成缩略图
            byte[] thumbnailBytes = imageProcessService.generateThumbnail(
                processedBytes, 
                ossConfig.getAvatar().getThumbnailSize()
            );
            
            // 9. 上传到OSS
            String originalKey = generateObjectKey("original", filePrefix, format);
            String processedKey = generateObjectKey("processed", filePrefix, "jpg");
            String thumbnailKey = generateObjectKey("thumbnails", filePrefix, "jpg");
            
            // 上传原始图片
            String originalUrl = ossService.uploadBytes(originalBytes, originalKey, file.getContentType());
            
            // 上传处理后的图片
            String avatarUrl = ossService.uploadBytes(processedBytes, processedKey, "image/jpeg");
            
            // 上传缩略图
            String thumbnailUrl = ossService.uploadBytes(thumbnailBytes, thumbnailKey, "image/jpeg");
            
            // 10. 更新用户头像URL
            User updateUser = new User();
            updateUser.setUserId(userId);
            updateUser.setAvatarUrl(avatarUrl);
            updateUser.setUpdatedAt(LocalDateTime.now());
            
            int updateCount = userMapper.updateById(updateUser);
            if (updateCount != 1) {
                log.error("[uploadAvatar] 更新用户头像URL失败, userId={}", userId);
                throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "更新用户头像失败");
            }
            
            // 11. 构建响应
            AvatarUploadResponse response = AvatarUploadResponse.builder()
                .avatarUrl(avatarUrl)
                .thumbnailUrl(thumbnailUrl)
                .originalUrl(originalUrl)
                .fileSize((long) processedBytes.length)
                .dimensions(dimensions[0] + "x" + dimensions[1])
                .build();
            
            log.info("[uploadAvatar] 头像上传成功, userId={}, avatarUrl={}", userId, avatarUrl);
            return response;
            
        } catch (Exception e) {
            log.error("[uploadAvatar] 头像上传失败, userId={}: {}", userId, e.getMessage(), e);
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "头像上传失败");
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAvatar(Long userId) {
        log.info("[deleteAvatar] 删除用户头像, userId={}", userId);
        
        // 1. 验证用户存在
        User user = userMapper.selectByUserId(userId);
        if (user == null) {
            log.error("[deleteAvatar] 用户不存在, userId={}", userId);
            throw BusinessException.of(ErrorCode.USER_NOT_FOUND);
        }
        
        try {
            // 2. 删除OSS中的头像文件
            deleteOldAvatar(user);
            
            // 3. 清空用户头像URL
            User updateUser = new User();
            updateUser.setUserId(userId);
            updateUser.setAvatarUrl(null);
            updateUser.setUpdatedAt(LocalDateTime.now());
            
            int updateCount = userMapper.updateById(updateUser);
            if (updateCount != 1) {
                log.error("[deleteAvatar] 清空用户头像URL失败, userId={}", userId);
                return false;
            }
            
            log.info("[deleteAvatar] 用户头像删除成功, userId={}", userId);
            return true;
            
        } catch (Exception e) {
            log.error("[deleteAvatar] 删除用户头像失败, userId={}: {}", userId, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public String getAvatarUrl(Long userId) {
        User user = userMapper.selectByUserId(userId);
        if (user == null) {
            return null;
        }
        return user.getAvatarUrl();
    }
    
    /**
     * 验证上传文件
     */
    private void validateFile(MultipartFile file) {
        // 检查文件是否为空
        if (file == null || file.isEmpty()) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "请选择要上传的头像文件");
        }
        
        // 检查文件大小
        if (file.getSize() > ossConfig.getAvatar().getMaxFileSize()) {
            long maxSizeMB = ossConfig.getAvatar().getMaxFileSize() / 1024 / 1024;
            throw BusinessException.of(ErrorCode.PARAM_ERROR, 
                "头像文件大小不能超过 " + maxSizeMB + "MB");
        }
        
        // 检查文件类型
        String contentType = file.getContentType();
        if (StrUtil.isBlank(contentType) || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, 
                "不支持的文件格式，请上传 JPG、PNG 或 WebP 格式的图片");
        }
        
        log.info("[validateFile] 文件验证通过, fileName={}, size={} bytes, contentType={}", 
            file.getOriginalFilename(), file.getSize(), contentType);
    }
    
    /**
     * 删除用户旧头像
     */
    private void deleteOldAvatar(User user) {
        if (StrUtil.isBlank(user.getAvatarUrl())) {
            return;
        }
        
        try {
            // 从URL中提取对象键
            String avatarUrl = user.getAvatarUrl();
            String objectKeyPrefix = extractObjectKeyPrefix(avatarUrl);
            
            if (StrUtil.isNotBlank(objectKeyPrefix)) {
                // 删除所有相关文件（原始图、处理图、缩略图）
                String originalKey = objectKeyPrefix.replace("processed/", "original/");
                String processedKey = objectKeyPrefix;
                String thumbnailKey = objectKeyPrefix.replace("processed/", "thumbnails/");
                
                ossService.deleteFile(originalKey);
                ossService.deleteFile(processedKey);
                ossService.deleteFile(thumbnailKey);
                
                log.info("[deleteOldAvatar] 删除旧头像文件成功, userId={}", user.getUserId());
            }
        } catch (Exception e) {
            log.warn("[deleteOldAvatar] 删除旧头像文件失败, userId={}: {}", 
                user.getUserId(), e.getMessage());
        }
    }
    
    /**
     * 生成对象键
     */
    private String generateObjectKey(String type, String filePrefix, String format) {
        return ossConfig.getAvatar().getBasePath() + 
               ossConfig.getAvatar().getProcessedPath().replace("processed/", type + "/") + 
               filePrefix + "." + format;
    }
    
    /**
     * 从URL中提取对象键前缀
     */
    private String extractObjectKeyPrefix(String url) {
        try {
            // 假设URL格式为: https://bucket.endpoint/avatars/processed/user123_20240101.jpg
            int index = url.indexOf(ossConfig.getAvatar().getBasePath());
            if (index != -1) {
                return url.substring(index);
            }
        } catch (Exception e) {
            log.warn("[extractObjectKeyPrefix] 提取对象键失败, url={}: {}", url, e.getMessage());
        }
        return null;
    }
}
