package com.carol.backend.service;

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
 * 音频文件保存服务
 */
@Slf4j
@Service
public class AudioFileService {
    
    private static final String AUDIO_FOLDER = "uploaded-audio";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    
    /**
     * 保存音频文件到resources文件夹
     */
    public String saveAudioFile(MultipartFile audioFile, String userAccount) throws IOException {
        log.info("[saveAudioFile] 开始保存音频文件: {}, 用户: {}, 大小: {} bytes", 
            audioFile.getOriginalFilename(), userAccount, audioFile.getSize());
        
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
        try {
            Files.copy(audioFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("[saveAudioFile] 音频文件保存成功: {}", filePath);
            
            // 5. 返回相对路径
            String relativePath = AUDIO_FOLDER + "/" + fileName;
            log.info("[saveAudioFile] 返回相对路径: {}", relativePath);
            return relativePath;
            
        } catch (IOException e) {
            log.error("[saveAudioFile] 保存音频文件失败: {}", e.getMessage(), e);
            throw new IOException("保存音频文件失败: " + e.getMessage(), e);
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
    
    /**
     * 清理旧的音频文件（可选功能）
     */
    public void cleanupOldFiles(int daysToKeep) {
        try {
            Path audioFolderPath = getResourcesPath().resolve(AUDIO_FOLDER);
            if (!Files.exists(audioFolderPath)) {
                return;
            }
            
            long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60 * 60 * 1000);
            
            Files.list(audioFolderPath)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis() < cutoffTime;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        log.info("[cleanupOldFiles] 删除旧文件: {}", path);
                    } catch (IOException e) {
                        log.warn("[cleanupOldFiles] 删除文件失败: {}", path, e);
                    }
                });
                
        } catch (IOException e) {
            log.error("[cleanupOldFiles] 清理旧文件失败", e);
        }
    }
    
    /**
     * 获取音频文件的完整路径
     */
    public Path getAudioFilePath(String relativePath) throws IOException {
        return getResourcesPath().resolve(relativePath);
    }
    
    /**
     * 检查音频文件是否存在
     */
    public boolean audioFileExists(String relativePath) {
        try {
            Path filePath = getAudioFilePath(relativePath);
            return Files.exists(filePath);
        } catch (IOException e) {
            log.error("[audioFileExists] 检查文件存在性失败: {}", relativePath, e);
            return false;
        }
    }
}
