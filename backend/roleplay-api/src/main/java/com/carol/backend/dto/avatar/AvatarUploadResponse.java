package com.carol.backend.dto.avatar;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 头像上传响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvatarUploadResponse {
    
    /**
     * 头像URL
     */
    private String avatarUrl;
    
    /**
     * 缩略图URL
     */
    private String thumbnailUrl;
    
    /**
     * 原始图片URL
     */
    private String originalUrl;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 图片尺寸
     */
    private String dimensions;
}
