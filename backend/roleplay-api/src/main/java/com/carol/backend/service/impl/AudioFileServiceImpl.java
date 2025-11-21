package com.carol.backend.service.impl;

import com.carol.backend.enums.ErrorCode;
import com.carol.backend.exception.BusinessException;
import com.carol.backend.service.IAudioFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 音频文件服务实现类
 * 
 * @author jianjl
 * @version 1.0
 * @description 音频文件保存和管理服务实现
 * @date 2025-01-15
 */
@Slf4j
@Service
public class AudioFileServiceImpl implements IAudioFileService {
    
    private static final String AUDIO_FOLDER = "uploaded-audio";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    
    @Override
    public String saveAudioFile(MultipartFile audioFile, String userAccount) throws IOException {
        log.info("[saveAudioFile] 开始保存音频文件: fileName={}, userAccount={}, size={} bytes", 
            audioFile.getOriginalFilename(), userAccount, audioFile.getSize());
        
        try {
            // 1. 获取resources目录路径
            Path resourcesPath = getResourcesPath();
            
            // 2. 创建音频文件夹
            Path audioFolderPath = resourcesPath.resolve(AUDIO_FOLDER);
            if (!Files.exists(audioFolderPath)) {
                Files.createDirectories(audioFolderPath);
                log.info("[saveAudioFile] 创建音频文件夹: {}", audioFolderPath);
            }
            
            // 3. 生成文件名
            String fileName = generateFileName(audioFile, userAccount);
            Path filePath = audioFolderPath.resolve(fileName);
            
            // 4. 保存文件
            Files.copy(audioFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("[saveAudioFile] 音频文件保存成功: {}", filePath);
            
            // 5. 返回相对路径
            String relativePath = AUDIO_FOLDER + "/" + fileName;
            log.info("[saveAudioFile] 返回相对路径: {}", relativePath);
            return relativePath;
            
        } catch (IOException e) {
            log.error("[saveAudioFile] 保存音频文件失败: fileName={}, error={}", 
                audioFile.getOriginalFilename(), e.getMessage(), e);
            throw BusinessException.of(ErrorCode.FILE_UPLOAD_FAILED, "保存音频文件失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void cleanupOldFiles(int daysToKeep) {
        log.info("[cleanupOldFiles] 开始清理旧文件: daysToKeep={}", daysToKeep);
        
        try {
            Path audioFolderPath = getResourcesPath().resolve(AUDIO_FOLDER);
            if (!Files.exists(audioFolderPath)) {
                log.info("[cleanupOldFiles] 音频文件夹不存在，跳过清理");
                return;
            }
            
            long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60 * 60 * 1000);
            int deletedCount = 0;
            
            try (var paths = Files.list(audioFolderPath)) {
                deletedCount = (int) paths
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toMillis() < cutoffTime;
                        } catch (IOException e) {
                            log.warn("[cleanupOldFiles] 获取文件修改时间失败: {}", path, e);
                            return false;
                        }
                    })
                    .peek(path -> {
                        try {
                            Files.delete(path);
                            log.debug("[cleanupOldFiles] 删除旧文件: {}", path);
                        } catch (IOException e) {
                            log.warn("[cleanupOldFiles] 删除文件失败: {}", path, e);
                        }
                    })
                    .count();
            }
            
            log.info("[cleanupOldFiles] 清理完成: deletedCount={}", deletedCount);
            
        } catch (IOException e) {
            log.error("[cleanupOldFiles] 清理旧文件失败: error={}", e.getMessage(), e);
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "清理旧文件失败", e);
        }
    }
    
    @Override
    public Path getAudioFilePath(String relativePath) throws IOException {
        log.debug("[getAudioFilePath] 获取音频文件路径: relativePath={}", relativePath);
        
        try {
            Path filePath = getResourcesPath().resolve(relativePath);
            log.debug("[getAudioFilePath] 完整路径: {}", filePath);
            return filePath;
        } catch (IOException e) {
            log.error("[getAudioFilePath] 获取音频文件路径失败: relativePath={}, error={}", 
                relativePath, e.getMessage(), e);
            throw BusinessException.of(ErrorCode.FILE_NOT_FOUND, "获取音频文件路径失败", e);
        }
    }
    
    @Override
    public boolean audioFileExists(String relativePath) {
        log.debug("[audioFileExists] 检查音频文件是否存在: relativePath={}", relativePath);
        
        try {
            Path filePath = getAudioFilePath(relativePath);
            boolean exists = Files.exists(filePath);
            log.debug("[audioFileExists] 文件存在性检查结果: relativePath={}, exists={}", relativePath, exists);
            return exists;
        } catch (Exception e) {
            log.error("[audioFileExists] 检查文件存在性失败: relativePath={}, error={}", 
                relativePath, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 生成唯一的文件名
     */
    private String generateFileName(MultipartFile audioFile, String userAccount) {
        String originalFilename = audioFile.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        
        // 生成时间戳
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        
        // 清理用户账号（移除特殊字符）
        String cleanUserAccount = userAccount != null ? 
            userAccount.replaceAll("[^a-zA-Z0-9]", "_") : "anonymous";
        
        // 格式: 用户账号_时间戳.扩展名
        String fileName = String.format("%s_%s.%s", cleanUserAccount, timestamp, extension);
        
        log.debug("[generateFileName] 生成文件名: {} -> {}", originalFilename, fileName);
        return fileName;
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "wav"; // 默认扩展名
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
    
    /**
     * 获取resources目录路径
     */
    private Path getResourcesPath() throws IOException {
        try {
            // 尝试获取resources目录的实际路径
            ClassPathResource resource = new ClassPathResource("");
            File resourcesDir = resource.getFile();
            return resourcesDir.toPath();
        } catch (IOException e) {
            // 如果是JAR包运行，使用项目根目录下的resources文件夹
            String userDir = System.getProperty("user.dir");
            Path projectPath = Paths.get(userDir);
            Path resourcesPath = projectPath.resolve("src/main/resources");
            
            // 如果开发环境的resources不存在，创建一个临时目录
            if (!Files.exists(resourcesPath)) {
                resourcesPath = projectPath.resolve("uploaded-resources");
                if (!Files.exists(resourcesPath)) {
                    Files.createDirectories(resourcesPath);
                }
            }
            
            log.info("[getResourcesPath] 使用resources路径: {}", resourcesPath);
            return resourcesPath;
        }
    }
}
