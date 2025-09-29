package com.carol.backend.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 阿里云OSS服务接口
 */
public interface IOssService {
    
    /**
     * 上传文件到OSS
     * 
     * @param file 文件
     * @param objectKey 对象键（文件路径）
     * @return 文件访问URL
     */
    String uploadFile(MultipartFile file, String objectKey);
    
    /**
     * 上传字节数组到OSS
     * 
     * @param bytes 字节数组
     * @param objectKey 对象键（文件路径）
     * @param contentType 内容类型
     * @return 文件访问URL
     */
    String uploadBytes(byte[] bytes, String objectKey, String contentType);
    
    /**
     * 删除OSS文件
     * 
     * @param objectKey 对象键（文件路径）
     * @return 是否删除成功
     */
    boolean deleteFile(String objectKey);
    
    /**
     * 检查文件是否存在
     * 
     * @param objectKey 对象键（文件路径）
     * @return 是否存在
     */
    boolean fileExists(String objectKey);
    
    /**
     * 生成预签名URL（用于临时访问）
     * 
     * @param objectKey 对象键（文件路径）
     * @param expireSeconds 过期时间（秒）
     * @return 预签名URL
     */
    String generatePresignedUrl(String objectKey, int expireSeconds);
}
