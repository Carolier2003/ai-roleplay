package com.carol.backend.service.impl;

import com.carol.backend.config.SpeechPerformanceConfig;
import com.carol.backend.dto.SpeechRecognitionRequest;
import com.carol.backend.service.ISpeechValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * 语音识别验证服务实现类
 * 
 * @author jianjl
 * @version 1.0
 * @description 语音识别验证服务实现
 * @date 2025-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechValidationServiceImpl implements ISpeechValidationService {
    
    private final SpeechPerformanceConfig performanceConfig;
    
    // 常见音频文件魔数
    private static final Pattern WAVE_HEADER = Pattern.compile("^RIFF.{4}WAVE", Pattern.DOTALL);
    private static final byte[] MP3_HEADER_1 = {(byte)0xFF, (byte)0xFB};
    private static final byte[] MP3_HEADER_2 = {(byte)0xFF, (byte)0xF3};
    private static final byte[] MP3_HEADER_3 = {(byte)0xFF, (byte)0xF2};
    private static final String OGG_HEADER = "OggS";
    
    @Override
    public ISpeechValidationService.ValidationResult validateAudioFile(MultipartFile file, SpeechRecognitionRequest request) {
        log.debug("[validateAudioFile] 开始验证音频文件: fileName={}, size={} bytes", 
                file.getOriginalFilename(), file.getSize());
        
        // 1. 文件大小验证
        ISpeechValidationService.ValidationResult sizeResult = validateFileSize(file);
        if (!sizeResult.isValid()) {
            return sizeResult;
        }
        
        // 2. 文件格式验证
        ISpeechValidationService.ValidationResult formatResult = validateFileFormat(file, request);
        if (!formatResult.isValid()) {
            return formatResult;
        }
        
        // 3. 参数验证
        ISpeechValidationService.ValidationResult paramResult = validateParameters(request);
        if (!paramResult.isValid()) {
            return paramResult;
        }
        
        // 4. 文件内容验证
        ISpeechValidationService.ValidationResult contentResult = validateFileContent(file, request);
        if (!contentResult.isValid()) {
            return contentResult;
        }
        
        log.debug("[validateAudioFile] 音频文件验证通过: fileName={}", file.getOriginalFilename());
        return ISpeechValidationService.ValidationResult.success();
    }
    
    /**
     * 验证文件大小
     */
    private ISpeechValidationService.ValidationResult validateFileSize(MultipartFile file) {
        long maxSizeBytes = performanceConfig.getFileLimit().getMaxFileSizeMB() * 1024 * 1024;
        
        if (file.isEmpty()) {
            return ISpeechValidationService.ValidationResult.failure("音频文件为空");
        }
        
        if (file.getSize() > maxSizeBytes) {
            return ISpeechValidationService.ValidationResult.failure(String.format(
                "音频文件过大，最大允许 %d MB，当前文件 %.2f MB",
                performanceConfig.getFileLimit().getMaxFileSizeMB(),
                file.getSize() / (1024.0 * 1024.0)
            ));
        }
        
        return ISpeechValidationService.ValidationResult.success();
    }
    
    /**
     * 验证文件格式
     */
    private ISpeechValidationService.ValidationResult validateFileFormat(MultipartFile file, SpeechRecognitionRequest request) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            return ISpeechValidationService.ValidationResult.failure("文件名不能为空");
        }
        
        // 从文件名提取格式
        String fileExtension = getFileExtension(filename).toLowerCase();
        String requestFormat = request.getFormat() != null ? request.getFormat().toLowerCase() : fileExtension;
        
        // 检查是否为支持的格式
        String[] allowedFormats = performanceConfig.getFileLimit().getAllowedFormats();
        boolean isAllowed = Arrays.stream(allowedFormats)
                .anyMatch(format -> format.equalsIgnoreCase(requestFormat));
        
        if (!isAllowed) {
            return ISpeechValidationService.ValidationResult.failure(String.format(
                "不支持的音频格式: %s，支持的格式: %s",
                requestFormat,
                String.join(", ", allowedFormats)
            ));
        }
        
        // 验证文件扩展名与请求格式一致
        if (!fileExtension.isEmpty() && !fileExtension.equals(requestFormat)) {
            log.warn("[validateFileFormat] 文件扩展名与请求格式不一致: fileExtension={}, requestFormat={}", 
                    fileExtension, requestFormat);
        }
        
        return ISpeechValidationService.ValidationResult.success();
    }
    
    /**
     * 验证参数
     */
    private ISpeechValidationService.ValidationResult validateParameters(SpeechRecognitionRequest request) {
        // 验证采样率
        if (request.getSampleRate() != null) {
            int sampleRate = request.getSampleRate();
            int minRate = performanceConfig.getFileLimit().getMinSampleRate();
            int maxRate = performanceConfig.getFileLimit().getMaxSampleRate();
            
            if (sampleRate < minRate || sampleRate > maxRate) {
                return ISpeechValidationService.ValidationResult.failure(String.format(
                    "采样率超出范围，支持范围: %d - %d Hz，当前: %d Hz",
                    minRate, maxRate, sampleRate
                ));
            }
        }
        
        // 验证模型名称
        String model = request.getModel();
        if (model != null && model.trim().isEmpty()) {
            return ISpeechValidationService.ValidationResult.failure("模型名称不能为空");
        }
        
        return ISpeechValidationService.ValidationResult.success();
    }
    
    /**
     * 验证文件内容
     */
    private ISpeechValidationService.ValidationResult validateFileContent(MultipartFile file, SpeechRecognitionRequest request) {
        try {
            byte[] header = new byte[Math.min(12, (int) file.getSize())];
            file.getInputStream().read(header);
            
            String format = request.getFormat();
            if (format == null) {
                format = getFileExtension(file.getOriginalFilename());
            }
            
            // 根据格式验证文件头
            switch (format.toLowerCase()) {
                case "wav":
                    if (!isValidWaveFile(header)) {
                        return ISpeechValidationService.ValidationResult.failure("无效的WAV文件格式");
                    }
                    break;
                case "mp3":
                    if (!isValidMp3File(header)) {
                        return ISpeechValidationService.ValidationResult.failure("无效的MP3文件格式");
                    }
                    break;
                case "ogg":
                case "opus":
                    if (!isValidOggFile(header)) {
                        return ISpeechValidationService.ValidationResult.failure("无效的OGG/Opus文件格式");
                    }
                    break;
                // 其他格式可以添加相应的验证
            }
            
        } catch (Exception e) {
            log.error("[validateFileContent] 验证文件内容时发生错误: error={}", e.getMessage(), e);
            return ISpeechValidationService.ValidationResult.failure("文件内容验证失败: " + e.getMessage());
        }
        
        return ISpeechValidationService.ValidationResult.success();
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
    
    /**
     * 验证WAV文件
     */
    private boolean isValidWaveFile(byte[] header) {
        if (header.length < 12) return false;
        String headerStr = new String(header, 0, Math.min(12, header.length));
        return headerStr.startsWith("RIFF") && headerStr.contains("WAVE");
    }
    
    /**
     * 验证MP3文件
     */
    private boolean isValidMp3File(byte[] header) {
        if (header.length < 2) return false;
        return (header[0] == (byte)0xFF && (header[1] == (byte)0xFB || 
                header[1] == (byte)0xF3 || header[1] == (byte)0xF2));
    }
    
    /**
     * 验证OGG文件
     */
    private boolean isValidOggFile(byte[] header) {
        if (header.length < 4) return false;
        String headerStr = new String(header, 0, 4);
        return headerStr.equals("OggS");
    }
    
}
