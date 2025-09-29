package com.carol.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云OSS配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssConfig {
    
    /**
     * OSS服务端点
     */
    private String endpoint;
    
    /**
     * 访问密钥ID
     */
    private String accessKeyId;
    
    /**
     * 访问密钥Secret
     */
    private String accessKeySecret;
    
    /**
     * 存储桶名称
     */
    private String bucketName;
    
    /**
     * 头像存储配置
     */
    private AvatarConfig avatar = new AvatarConfig();
    
    @Data
    public static class AvatarConfig {
        /**
         * 基础路径
         */
        private String basePath = "avatars/";
        
        /**
         * 原始图片路径
         */
        private String originalPath = "original/";
        
        /**
         * 处理后图片路径
         */
        private String processedPath = "processed/";
        
        /**
         * 缩略图路径
         */
        private String thumbnailPath = "thumbnails/";
        
        /**
         * 最大文件大小（字节）
         */
        private Long maxFileSize = 10485760L; // 10MB
        
        /**
         * 允许的文件格式
         */
        private String allowedFormats = "jpg,jpeg,png,webp";
        
        /**
         * 缩略图尺寸
         */
        private Integer thumbnailSize = 150;
        
        /**
         * 处理后图片尺寸
         */
        private Integer processedSize = 400;
    }
}
