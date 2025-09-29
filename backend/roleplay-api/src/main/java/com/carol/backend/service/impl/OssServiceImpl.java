package com.carol.backend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.carol.backend.config.OssConfig;
import com.carol.backend.enums.ErrorCode;
import com.carol.backend.exception.BusinessException;
import com.carol.backend.service.IOssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;

/**
 * 阿里云OSS服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssServiceImpl implements IOssService {
    
    private final OssConfig ossConfig;
    private OSS ossClient;
    
    @PostConstruct
    public void init() {
        log.info("[init] 初始化阿里云OSS客户端, endpoint={}, bucketName={}", 
            ossConfig.getEndpoint(), ossConfig.getBucketName());
        
        // 验证配置
        if (StrUtil.isBlank(ossConfig.getEndpoint()) || 
            StrUtil.isBlank(ossConfig.getAccessKeyId()) || 
            StrUtil.isBlank(ossConfig.getAccessKeySecret()) || 
            StrUtil.isBlank(ossConfig.getBucketName())) {
            log.error("[init] 阿里云OSS配置不完整");
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "阿里云OSS配置不完整");
        }
        
        try {
            ossClient = new OSSClientBuilder().build(
                ossConfig.getEndpoint(), 
                ossConfig.getAccessKeyId(), 
                ossConfig.getAccessKeySecret()
            );
            
            // 检查存储桶是否存在
            if (!ossClient.doesBucketExist(ossConfig.getBucketName())) {
                log.error("[init] 存储桶不存在: {}", ossConfig.getBucketName());
                throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "OSS存储桶不存在");
            }
            
            log.info("[init] 阿里云OSS客户端初始化成功");
        } catch (Exception e) {
            log.error("[init] 初始化阿里云OSS客户端失败: {}", e.getMessage(), e);
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "初始化OSS客户端失败");
        }
    }
    
    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            log.info("[destroy] 关闭阿里云OSS客户端");
            ossClient.shutdown();
        }
    }
    
    @Override
    public String uploadFile(MultipartFile file, String objectKey) {
        log.info("[uploadFile] 上传文件到OSS, objectKey={}, fileName={}, size={} bytes", 
            objectKey, file.getOriginalFilename(), file.getSize());
        
        try {
            // 设置文件元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            metadata.setCacheControl("max-age=31536000"); // 缓存1年
            
            // 上传文件
            ossClient.putObject(ossConfig.getBucketName(), objectKey, file.getInputStream(), metadata);
            
            // 生成访问URL
            String fileUrl = generateFileUrl(objectKey);
            log.info("[uploadFile] 文件上传成功, objectKey={}, url={}", objectKey, fileUrl);
            
            return fileUrl;
        } catch (IOException e) {
            log.error("[uploadFile] 上传文件失败, objectKey={}: {}", objectKey, e.getMessage(), e);
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        } catch (Exception e) {
            log.error("[uploadFile] OSS上传异常, objectKey={}: {}", objectKey, e.getMessage(), e);
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "OSS上传异常");
        }
    }
    
    @Override
    public String uploadBytes(byte[] bytes, String objectKey, String contentType) {
        log.info("[uploadBytes] 上传字节数组到OSS, objectKey={}, size={} bytes, contentType={}", 
            objectKey, bytes.length, contentType);
        
        try {
            // 设置文件元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(bytes.length);
            metadata.setContentType(contentType);
            metadata.setCacheControl("max-age=31536000"); // 缓存1年
            
            // 上传文件
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ossClient.putObject(ossConfig.getBucketName(), objectKey, inputStream, metadata);
            
            // 生成访问URL
            String fileUrl = generateFileUrl(objectKey);
            log.info("[uploadBytes] 字节数组上传成功, objectKey={}, url={}", objectKey, fileUrl);
            
            return fileUrl;
        } catch (Exception e) {
            log.error("[uploadBytes] OSS上传异常, objectKey={}: {}", objectKey, e.getMessage(), e);
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "OSS上传异常");
        }
    }
    
    @Override
    public boolean deleteFile(String objectKey) {
        log.info("[deleteFile] 删除OSS文件, objectKey={}", objectKey);
        
        try {
            if (!fileExists(objectKey)) {
                log.warn("[deleteFile] 文件不存在, objectKey={}", objectKey);
                return true;
            }
            
            ossClient.deleteObject(ossConfig.getBucketName(), objectKey);
            log.info("[deleteFile] 文件删除成功, objectKey={}", objectKey);
            return true;
        } catch (Exception e) {
            log.error("[deleteFile] 删除文件失败, objectKey={}: {}", objectKey, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean fileExists(String objectKey) {
        try {
            return ossClient.doesObjectExist(ossConfig.getBucketName(), objectKey);
        } catch (Exception e) {
            log.error("[fileExists] 检查文件存在性失败, objectKey={}: {}", objectKey, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public String generatePresignedUrl(String objectKey, int expireSeconds) {
        log.info("[generatePresignedUrl] 生成预签名URL, objectKey={}, expireSeconds={}", 
            objectKey, expireSeconds);
        
        try {
            Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000L);
            URL url = ossClient.generatePresignedUrl(ossConfig.getBucketName(), objectKey, expiration);
            
            String presignedUrl = url.toString();
            log.info("[generatePresignedUrl] 预签名URL生成成功, objectKey={}", objectKey);
            
            return presignedUrl;
        } catch (Exception e) {
            log.error("[generatePresignedUrl] 生成预签名URL失败, objectKey={}: {}", 
                objectKey, e.getMessage(), e);
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "生成预签名URL失败");
        }
    }
    
    /**
     * 生成文件访问URL
     */
    private String generateFileUrl(String objectKey) {
        // 如果endpoint包含协议，直接使用；否则添加https协议
        String endpoint = ossConfig.getEndpoint();
        if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
            endpoint = "https://" + endpoint;
        }
        
        // 构建文件URL: https://bucket-name.endpoint/objectKey
        String bucketDomain = ossConfig.getBucketName() + "." + 
            endpoint.replace("https://", "").replace("http://", "");
        
        return "https://" + bucketDomain + "/" + objectKey;
    }
}
