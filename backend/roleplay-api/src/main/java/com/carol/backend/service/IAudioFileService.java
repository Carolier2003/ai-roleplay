package com.carol.backend.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 音频文件服务接口
 * 
 * @author jianjl
 * @version 1.0
 * @description 音频文件保存和管理服务
 * @date 2025-01-15
 */
public interface IAudioFileService {
    
    /**
     * 保存音频文件到resources文件夹
     * 
     * @param audioFile 音频文件
     * @param userAccount 用户账号
     * @return 相对路径
     * @throws IOException IO异常
     */
    String saveAudioFile(MultipartFile audioFile, String userAccount) throws IOException;
    
    /**
     * 清理旧的音频文件
     * 
     * @param daysToKeep 保留天数
     */
    void cleanupOldFiles(int daysToKeep);
    
    /**
     * 获取音频文件的完整路径
     * 
     * @param relativePath 相对路径
     * @return 完整路径
     * @throws IOException IO异常
     */
    Path getAudioFilePath(String relativePath) throws IOException;
    
    /**
     * 检查音频文件是否存在
     * 
     * @param relativePath 相对路径
     * @return 是否存在
     */
    boolean audioFileExists(String relativePath);
}
