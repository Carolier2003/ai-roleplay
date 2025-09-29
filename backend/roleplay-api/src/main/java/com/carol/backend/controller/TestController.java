package com.carol.backend.controller;

import com.carol.backend.config.OssConfig;
import com.carol.backend.service.IOssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器 - 用于验证各种配置和服务
 * 注意：这是测试接口，生产环境应该移除或加上认证
 */
@Slf4j
@RestController
@RequestMapping("/api/public/test")
@RequiredArgsConstructor
public class TestController {
    
    private final OssConfig ossConfig;
    private final IOssService ossService;
    
    /**
     * 测试阿里云OSS配置
     */
    @GetMapping("/oss-config")
    public Map<String, Object> testOssConfig() {
        log.info("[testOssConfig] 测试阿里云OSS配置");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 返回配置信息（隐藏敏感信息）
            result.put("endpoint", ossConfig.getEndpoint());
            result.put("bucketName", ossConfig.getBucketName());
            result.put("accessKeyId", maskSensitiveInfo(ossConfig.getAccessKeyId()));
            result.put("accessKeySecret", maskSensitiveInfo(ossConfig.getAccessKeySecret()));
            
            // 头像配置
            Map<String, Object> avatarConfig = new HashMap<>();
            avatarConfig.put("basePath", ossConfig.getAvatar().getBasePath());
            avatarConfig.put("originalPath", ossConfig.getAvatar().getOriginalPath());
            avatarConfig.put("processedPath", ossConfig.getAvatar().getProcessedPath());
            avatarConfig.put("thumbnailPath", ossConfig.getAvatar().getThumbnailPath());
            avatarConfig.put("maxFileSize", ossConfig.getAvatar().getMaxFileSize());
            avatarConfig.put("allowedFormats", ossConfig.getAvatar().getAllowedFormats());
            avatarConfig.put("thumbnailSize", ossConfig.getAvatar().getThumbnailSize());
            avatarConfig.put("processedSize", ossConfig.getAvatar().getProcessedSize());
            
            result.put("avatarConfig", avatarConfig);
            result.put("status", "success");
            result.put("message", "OSS配置读取成功");
            
            log.info("[testOssConfig] OSS配置测试成功");
            
        } catch (Exception e) {
            log.error("[testOssConfig] OSS配置测试失败: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("message", "OSS配置测试失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 测试OSS连接
     */
    @GetMapping("/oss-connection")
    public Map<String, Object> testOssConnection() {
        log.info("[testOssConnection] 测试阿里云OSS连接");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 测试文件是否存在（使用一个不存在的测试文件）
            String testKey = "test/connection-test.txt";
            boolean exists = ossService.fileExists(testKey);
            
            result.put("status", "success");
            result.put("message", "OSS连接测试成功");
            result.put("testKey", testKey);
            result.put("fileExists", exists);
            result.put("endpoint", ossConfig.getEndpoint());
            result.put("bucketName", ossConfig.getBucketName());
            
            log.info("[testOssConnection] OSS连接测试成功");
            
        } catch (Exception e) {
            log.error("[testOssConnection] OSS连接测试失败: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("message", "OSS连接测试失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
        }
        
        return result;
    }
    
    /**
     * 掩码敏感信息
     */
    private String maskSensitiveInfo(String info) {
        if (info == null || info.length() <= 8) {
            return "****";
        }
        return info.substring(0, 4) + "****" + info.substring(info.length() - 4);
    }
}
