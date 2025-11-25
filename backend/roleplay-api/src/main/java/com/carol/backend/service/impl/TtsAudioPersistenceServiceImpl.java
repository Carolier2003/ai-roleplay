package com.carol.backend.service.impl;

import com.carol.backend.enums.ErrorCode;
import com.carol.backend.exception.BusinessException;
import com.carol.backend.service.IOssService;
import com.carol.backend.service.ITtsAudioPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * TTS音频持久化服务实现类
 * 
 * @author jianjl
 * @version 1.0
 * @description 负责将阿里云TTS返回的临时URL音频下载并上传到OSS
 * @date 2025-01-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TtsAudioPersistenceServiceImpl implements ITtsAudioPersistenceService {
    
    private final IOssService ossService;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    private static final String AUDIO_CATEGORY = "audio/tts";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    
    @Override
    public com.carol.backend.dto.TtsPersistenceResult persistTtsAudio(String temporaryUrl, Long userId, Long characterId) {
        log.info("[persistTtsAudio] 开始持久化TTS音频: userId={}, characterId={}, temporaryUrl={}", 
                userId, characterId, temporaryUrl);
        
        try {
            // 1. 下载临时URL的音频数据
            byte[] audioBytes = downloadAudio(temporaryUrl);
            log.info("[persistTtsAudio] 音频下载成功: size={} bytes", audioBytes.length);
            
            // 计算音频时长 (WAV格式: 24000Hz, 16bit, 单声道)
            // 44字节头信息
            // 每秒字节数 = 24000 * 16 / 8 * 1 = 48000
            int duration = 0;
            if (audioBytes.length > 44) {
                duration = (int) Math.ceil((audioBytes.length - 44) / 48000.0);
            }
            log.info("[persistTtsAudio] 计算音频时长: {}秒", duration);
            
            // 2. 生成OSS对象键
            String objectKey = generateOssObjectKey(userId, characterId);
            log.info("[persistTtsAudio] 生成OSS对象键: {}", objectKey);
            
            // 3. 上传到OSS
            String ossUrl = ossService.uploadBytes(audioBytes, objectKey, "audio/wav");
            log.info("[persistTtsAudio] TTS音频上传OSS成功: userId={}, characterId={}, ossUrl={}", 
                    userId, characterId, ossUrl);
            
            return com.carol.backend.dto.TtsPersistenceResult.builder()
                    .audioUrl(ossUrl)
                    .duration(duration)
                    .build();
            
        } catch (Exception e) {
            log.error("[persistTtsAudio] TTS音频持久化失败: userId={}, characterId={}, temporaryUrl={}, error={}", 
                    userId, characterId, temporaryUrl, e.getMessage(), e);
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "TTS音频持久化失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 下载音频数据
     */
    private byte[] downloadAudio(String url) throws IOException, InterruptedException {
        log.debug("[downloadAudio] 开始下载音频: url={}", url);
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            
            if (response.statusCode() != 200) {
                throw new IOException("下载音频失败，HTTP状态码: " + response.statusCode());
            }
            
            byte[] audioBytes = response.body();
            log.debug("[downloadAudio] 音频下载成功: size={} bytes, contentType={}", 
                    audioBytes.length, response.headers().firstValue("Content-Type").orElse("unknown"));
            
            return audioBytes;
            
        } catch (IOException | InterruptedException e) {
            log.error("[downloadAudio] 下载音频失败: url={}, error={}", url, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 生成OSS对象键
     * 格式: audio/tts/{userId}/char_{characterId}/{timestamp}.wav
     * 或: audio/tts/{userId}/{timestamp}.wav (无角色时)
     */
    private String generateOssObjectKey(Long userId, Long characterId) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String userIdStr = userId != null ? userId.toString() : "anonymous";
        
        if (characterId != null) {
            return String.format("%s/%s/char_%d/%s.wav", 
                    AUDIO_CATEGORY, userIdStr, characterId, timestamp);
        } else {
            return String.format("%s/%s/%s.wav", 
                    AUDIO_CATEGORY, userIdStr, timestamp);
        }
    }
}
