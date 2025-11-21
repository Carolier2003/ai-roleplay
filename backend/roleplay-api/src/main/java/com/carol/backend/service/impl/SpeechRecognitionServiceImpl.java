package com.carol.backend.service.impl;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.carol.backend.config.SpeechRecognitionConfig;
import com.carol.backend.dto.SpeechRecognitionRequest;
import com.carol.backend.dto.SpeechRecognitionResponse;
import com.carol.backend.enums.ErrorCode;
import com.carol.backend.exception.BusinessException;
import com.carol.backend.service.ISpeechRecognitionService;
import com.carol.backend.service.ISpeechValidationService;
import com.carol.backend.service.ISpeechMetricsCollector;
import com.carol.backend.service.ISpeechTimeoutManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 同步语音识别服务实现类
 * 
 * @author jianjl
 * @version 1.0
 * @description 同步语音识别服务实现
 * @date 2025-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechRecognitionServiceImpl implements ISpeechRecognitionService {
    
    private final SpeechRecognitionConfig config;
    private final ISpeechValidationService validationService;
    private final ISpeechMetricsCollector metricsCollector;
    private final ISpeechTimeoutManager timeoutManager;
    
    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;
    
    /**
     * 初始化时设置全局API密钥
     */
    @PostConstruct
    private void initializeApiKey() {
        System.setProperty("dashscope.api-key", apiKey);
        log.debug("[initializeApiKey] ASR服务已设置DashScope API密钥系统属性");
    }
    
    @Override
    public SpeechRecognitionResponse recognizeFile(MultipartFile audioFile, SpeechRecognitionRequest request) 
            throws IOException, NoApiKeyException {
        
        log.info("[recognizeFile] 开始同步识别音频文件: fileName={}, size={} bytes", 
                audioFile.getOriginalFilename(), audioFile.getSize());
        
        // 1. 使用验证服务验证文件
        ISpeechValidationService.ValidationResult validation = validationService.validateAudioFile(audioFile, request);
        if (!validation.isValid()) {
            log.warn("[recognizeFile] 音频文件验证失败: errorMessage={}", validation.getErrorMessage());
            throw BusinessException.of(ErrorCode.PARAM_ERROR, validation.getErrorMessage());
        }
        
        // 2. 开始性能监控
        ISpeechMetricsCollector.RequestContext metricsContext = metricsCollector.recordRequestStart("sync", audioFile.getSize());
        
        // 保存临时文件
        File tempFile = saveToTempFile(audioFile);
        
        try {
            // 3. 使用超时控制执行识别
            return timeoutManager.executeWithTimeout(() -> {
                try {
                    // 创建识别器
                    Recognition recognizer = new Recognition();
                    
                    // 构建参数
                    RecognitionParam param = buildRecognitionParam(request);
                    
                    // 执行识别
                    String result = recognizer.call(param, tempFile);
                    
                    // 计算音频时长
                    Double audioDuration = calculateAudioDuration(tempFile);
                    
                    // 构建响应
                    SpeechRecognitionResponse response = SpeechRecognitionResponse.builder()
                            .requestId(recognizer.getLastRequestId())
                            .text(result)
                            .confidence(0.95) // 默认置信度，将来可从DashScope结果中提取
                            .isSentenceEnd(true)
                            .firstPackageDelay(recognizer.getFirstPackageDelay())
                            .lastPackageDelay(recognizer.getLastPackageDelay())
                            .duration(audioDuration)
                            .timestamp(LocalDateTime.now())
                            .build();
                    
                    log.info("同步识别完成, requestId: {}, 识别结果: {}", response.getRequestId(), result);
                    
                    // 关闭连接
                    recognizer.getDuplexApi().close(1000, "任务完成");
                    
                    // 记录成功
                    metricsCollector.recordRequestComplete(metricsContext, true, null);
                    
                    return response;
                    
                } catch (Exception e) {
                    log.error("同步识别执行失败", e);
                    throw new RuntimeException(e);
                }
            }, "sync");
            
        } catch (ISpeechTimeoutManager.TimeoutException e) {
            log.error("同步识别超时", e);
            metricsCollector.recordRequestComplete(metricsContext, false, "TIMEOUT");
            throw new RuntimeException("语音识别超时: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("语音识别失败", e);
            metricsCollector.recordRequestComplete(metricsContext, false, e.getClass().getSimpleName());
            throw new RuntimeException("语音识别失败: " + e.getMessage(), e);
        } finally {
            // 清理临时文件
            cleanupTempFile(tempFile);
        }
    }
    
    @Override
    public CompletableFuture<SpeechRecognitionResponse> recognizeFileAsync(MultipartFile audioFile, SpeechRecognitionRequest request) {
        log.info("[recognizeFileAsync] 开始异步识别音频文件: fileName={}, size={} bytes", 
                audioFile.getOriginalFilename(), audioFile.getSize());
        
        // 使用带超时的异步执行
        return timeoutManager.executeAsyncWithTimeout(() -> {
            try {
                return recognizeFile(audioFile, request);
            } catch (Exception e) {
                log.error("异步语音识别失败", e);
                throw new RuntimeException(e);
            }
        }, "async");
    }
    
    /**
     * 验证音频文件
     */
    private void validateAudioFile(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new IllegalArgumentException("音频文件不能为空");
        }
        
        // 检查文件大小
        long maxSizeBytes = config.getMaxFileSizeMB() * 1024 * 1024L;
        if (audioFile.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException(
                String.format("音频文件大小不能超过 %dMB", config.getMaxFileSizeMB()));
        }
        
        // 检查文件扩展名
        String originalFilename = audioFile.getOriginalFilename();
        if (originalFilename == null || !isValidAudioFormat(originalFilename)) {
            throw new IllegalArgumentException("不支持的音频格式，支持格式: " + 
                String.join(", ", config.getSupportedFormats()));
        }
    }
    
    /**
     * 检查是否为有效的音频格式
     */
    private boolean isValidAudioFormat(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        for (String format : config.getSupportedFormats()) {
            if (format.equals(extension)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex + 1) : "";
    }
    
    /**
     * 保存上传文件到临时文件
     */
    private File saveToTempFile(MultipartFile audioFile) throws IOException {
        String originalFilename = audioFile.getOriginalFilename();
        String extension = originalFilename != null ? getFileExtension(originalFilename) : "tmp";
        
        Path tempFile = Files.createTempFile("speech_recognition_", "." + extension);
        Files.copy(audioFile.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
        
        log.debug("[saveToTempFile] 音频文件已保存到临时路径: path={}", tempFile.toString());
        return tempFile.toFile();
    }
    
    /**
     * 清理临时文件
     */
    private void cleanupTempFile(File tempFile) {
        if (tempFile != null && tempFile.exists()) {
            try {
                Files.delete(tempFile.toPath());
                log.debug("[cleanupTempFile] 临时文件已清理: path={}", tempFile.getPath());
            } catch (IOException e) {
                log.warn("[cleanupTempFile] 清理临时文件失败: path={}, error={}", tempFile.getPath(), e.getMessage(), e);
            }
        }
    }
    
    /**
     * 构建识别参数
     */
    private RecognitionParam buildRecognitionParam(SpeechRecognitionRequest request) throws NoApiKeyException {
        // 使用正确的Builder模式
        RecognitionParam.RecognitionParamBuilder<?, ?> builder = RecognitionParam.builder()
                .apiKey(apiKey)
                .model(request.getModel() != null ? request.getModel() : config.getDefaultModel())
                .format(request.getFormat() != null ? request.getFormat() : config.getDefaultFormat())
                .sampleRate(request.getSampleRate() != null ? request.getSampleRate() : config.getDefaultSampleRate());
        
        // 设置热词ID
        if (request.getVocabularyId() != null) {
            builder.vocabularyId(request.getVocabularyId());
        }
        if (request.getPhraseId() != null) {
            builder.phraseId(request.getPhraseId());
        }
        
        // 设置其他参数
        Map<String, Object> parameters = request.toParameterMap();
        if (!parameters.isEmpty()) {
            builder.parameters(parameters);
        }
        
        return builder.build();
    }
    
    /**
     * 转换Word列表为WordTimestamp列表
     * 注意：由于SDK版本限制，暂时返回空列表，实际项目中可根据具体需求实现
     */
    private List<SpeechRecognitionResponse.WordTimestamp> convertWords(Object words) {
        // 由于SDK版本的API差异，暂时返回空列表
        // 在实际使用中，可以根据具体的SDK版本和API结构来实现词级别时间戳
        return List.of();
    }
    
    /**
     * 计算音频文件时长（秒）
     */
    private Double calculateAudioDuration(File audioFile) {
        try {
            // 简单的基于文件大小的估算方法
            // 对于WAV文件：时长 ≈ 文件大小 / (采样率 × 位深度 × 声道数 / 8)
            // 对于常见的16kHz, 16bit, 单声道WAV：时长 ≈ 文件大小 / 32000
            
            long fileSize = audioFile.length();
            String fileName = audioFile.getName().toLowerCase();
            
            if (fileName.endsWith(".wav")) {
                // WAV文件：假设16kHz, 16bit, 单声道
                // 每秒数据量 = 16000 * 16 / 8 = 32000 bytes
                return (double) fileSize / 32000.0;
            } else if (fileName.endsWith(".mp3")) {
                // MP3文件：假设128kbps码率
                // 每秒数据量 = 128 * 1000 / 8 = 16000 bytes
                return (double) fileSize / 16000.0;
            } else {
                // 其他格式：使用WAV的估算方法
                return (double) fileSize / 32000.0;
            }
        } catch (Exception e) {
            log.warn("[calculateAudioDuration] 计算音频时长失败: {}", e.getMessage());
            // 如果计算失败，返回基于文件大小的粗略估算（假设每秒32KB）
            return (double) audioFile.length() / 32000.0;
        }
    }
}
