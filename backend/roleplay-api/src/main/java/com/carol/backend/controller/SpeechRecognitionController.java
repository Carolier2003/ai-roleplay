package com.carol.backend.controller;

import com.carol.backend.dto.ApiResponse;
import com.carol.backend.dto.SpeechRecognitionRequest;
import com.carol.backend.dto.SpeechRecognitionResponse;
import com.carol.backend.service.IAudioFileService;
import com.carol.backend.service.ISpeechRecognitionService;
import com.carol.backend.service.IStreamingSpeechRecognitionService;
import com.carol.backend.service.IOssService;
import com.carol.backend.service.ICustomMessageStorageService;
import com.carol.backend.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import java.util.concurrent.CompletableFuture;

/**
 * 语音识别控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/speech")
@RequiredArgsConstructor
@Validated
public class SpeechRecognitionController {
    
    private final ISpeechRecognitionService speechRecognitionService;
    private final IStreamingSpeechRecognitionService streamingService;
    private final IAudioFileService audioFileService;
    private final IOssService ossService;
    private final ICustomMessageStorageService customMessageStorageService;
    
    /**
     * 同步语音识别 - 上传文件进行识别
     */
    @PostMapping(value = "/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SpeechRecognitionResponse>> recognizeAudio(
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam(value = "model", required = false, defaultValue = "fun-asr-realtime") String model,
            @RequestParam(value = "format", required = false) String format,
            @RequestParam(value = "sampleRate", required = false, defaultValue = "16000") Integer sampleRate,
            @RequestParam(value = " semanticPunctuationEnabled", required = false, defaultValue = "false") Boolean semanticPunctuationEnabled,
            @RequestParam(value = "punctuationPredictionEnabled", required = false, defaultValue = "true") Boolean punctuationPredictionEnabled,
            @RequestParam(value = "maxSentenceSilence", required = false, defaultValue = "1300") Integer maxSentenceSilence,
            @RequestParam(value = "languageHints", required = false) String[] languageHints,
            @RequestParam(value = "conversationId", required = false) String conversationId,
            @RequestParam(value = "characterId", required = false) Long characterId) {
        
        try {
            log.info("收到同步语音识别请求，文件: {}, 大小: {} bytes", 
                audioFile.getOriginalFilename(), audioFile.getSize());
            
            // 获取当前用户账号
            String userAccount = SecurityUtils.getCurrentUserAccount();
            log.info("当前用户: {}", userAccount != null ? userAccount : "游客");
            
            // 保存音频文件到resources文件夹
            String savedFilePath = null;
            try {
                savedFilePath = audioFileService.saveAudioFile(audioFile, userAccount);
                log.info("音频文件已保存: {}", savedFilePath);
            } catch (Exception e) {
                log.warn("保存音频文件失败，继续进行识别: {}", e.getMessage());
            }
            
            // 如果格式未指定，从文件名推导
            if (format == null && audioFile.getOriginalFilename() != null) {
                format = getFormatFromFilename(audioFile.getOriginalFilename());
            }
            
            // 构建请求参数
            SpeechRecognitionRequest request = SpeechRecognitionRequest.builder()
                    .model(model)
                    .format(format)
                    .sampleRate(sampleRate)
                    .semanticPunctuationEnabled(semanticPunctuationEnabled)
                    .punctuationPredictionEnabled(punctuationPredictionEnabled)
                    .maxSentenceSilence(maxSentenceSilence)
                    .languageHints(languageHints)
                    .build();
            
            // 执行识别
            SpeechRecognitionResponse response = speechRecognitionService.recognizeFile(audioFile, request);
            
            // 上传音频到OSS并保存到Redis会话记忆
            String ossAudioUrl = null;
            Integer audioDuration = null;
            
            try {
                // 生成OSS对象键: audio/user/{userId}/{timestamp}.{ext}
                String userId = userAccount != null ? userAccount : "anonymous";
                String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
                String extension = getFormatFromFilename(audioFile.getOriginalFilename());
                String objectKey = String.format("audio/user/%s/%s.%s", userId, timestamp, extension);
                
                // 上传到OSS
                ossAudioUrl = ossService.uploadFile(audioFile, objectKey);
                log.info("[recognizeAudio] 用户音频已上传到OSS: ossUrl={}", ossAudioUrl);
                
                // 计算音频时长（简化版本：根据文件大小估算，后续可以用音频库精确计算）
                // 假设16kHz采样率，16bit，单声道：32000 bytes/sec
                audioDuration = (int) (audioFile.getSize() / 32000);
                if (audioDuration < 1) audioDuration = 1; // 最小1秒
                
                // 设置响应中的音频URL和时长
                response.setAudioUrl(ossAudioUrl);
                response.setAudioDuration(audioDuration);
                
            } catch (Exception ossError) {
                log.error("[recognizeAudio] 上传音频到OSS失败，继续返回识别结果: error={}", ossError.getMessage());
                // OSS上传失败不影响识别结果返回
            }
            
            // 如果有conversationId和characterId，保存用户消息到Redis
            if (conversationId != null && characterId != null && response.getText() != null) {
                try {
                    org.springframework.ai.chat.messages.UserMessage userMessage = 
                        new org.springframework.ai.chat.messages.UserMessage(response.getText());
                    
                    customMessageStorageService.saveMessage(
                        conversationId, 
                        userMessage, 
                        true,  // isUser = true
                        ossAudioUrl,
                        audioDuration
                    );
                    
                    log.info("[recognizeAudio] 用户语音消息已保存到Redis: conversationId={}, text={}, audioUrl={}", 
                            conversationId, response.getText(), ossAudioUrl);
                } catch (Exception redisError) {
                    log.error("[recognizeAudio] 保存用户消息到Redis失败: conversationId={}, error={}", 
                            conversationId, redisError.getMessage());
                    // Redis保存失败不影响识别结果返回
                }
            }
            
            return ResponseEntity.ok(ApiResponse.success(response, "语音识别完成"));
            
        } catch (Exception e) {
            log.error("同步语音识别失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("语音识别失败: " + e.getMessage()));
        }
    }
    
    /**
     * 异步语音识别 - 上传文件进行异步识别
     */
    @PostMapping(value = "/recognize/async", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CompletableFuture<SpeechRecognitionResponse>>> recognizeAudioAsync(
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam(value = "model", required = false, defaultValue = "fun-asr-realtime") String model,
            @RequestParam(value = "format", required = false) String format,
            @RequestParam(value = "sampleRate", required = false, defaultValue = "16000") Integer sampleRate,
            @RequestParam(value = "semanticPunctuationEnabled", required = false, defaultValue = "false") Boolean semanticPunctuationEnabled,
            @RequestParam(value = "punctuationPredictionEnabled", required = false, defaultValue = "true") Boolean punctuationPredictionEnabled,
            @RequestParam(value = "maxSentenceSilence", required = false, defaultValue = "1300") Integer maxSentenceSilence,
            @RequestParam(value = "languageHints", required = false) String[] languageHints) {
        
        try {
            log.info("收到异步语音识别请求，文件: {}, 大小: {} bytes", 
                audioFile.getOriginalFilename(), audioFile.getSize());
            
            // 获取当前用户账号
            String userAccount = SecurityUtils.getCurrentUserAccount();
            log.info("当前用户: {}", userAccount != null ? userAccount : "游客");
            
            // 保存音频文件到resources文件夹
            String savedFilePath = null;
            try {
                savedFilePath = audioFileService.saveAudioFile(audioFile, userAccount);
                log.info("音频文件已保存: {}", savedFilePath);
            } catch (Exception e) {
                log.warn("保存音频文件失败，继续进行识别: {}", e.getMessage());
            }
            
            // 如果格式未指定，从文件名推导
            if (format == null && audioFile.getOriginalFilename() != null) {
                format = getFormatFromFilename(audioFile.getOriginalFilename());
            }
            
            // 构建请求参数
            SpeechRecognitionRequest request = SpeechRecognitionRequest.builder()
                    .model(model)
                    .format(format)
                    .sampleRate(sampleRate)
                    .semanticPunctuationEnabled(semanticPunctuationEnabled)
                    .punctuationPredictionEnabled(punctuationPredictionEnabled)
                    .maxSentenceSilence(maxSentenceSilence)
                    .languageHints(languageHints)
                    .build();
            
            // 执行异步识别
            CompletableFuture<SpeechRecognitionResponse> future = 
                speechRecognitionService.recognizeFileAsync(audioFile, request);
            
            return ResponseEntity.ok(ApiResponse.success(future, "异步语音识别任务已提交"));
            
        } catch (Exception e) {
            log.error("异步语音识别失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("异步语音识别失败: " + e.getMessage()));
        }
    }
    
    /**
     * 创建流式语音识别会话
     */
    @PostMapping("/streaming/create")
    public SseEmitter createStreamingSession(@Valid @RequestBody SpeechRecognitionRequest request) {
        try {
            log.info("创建流式语音识别会话，模型: {}, 格式: {}", request.getModel(), request.getFormat());
            
            // 创建SSE连接
            SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时
            
            // 创建会话
            String sessionId = streamingService.createStreamingSession(request, emitter);
            
            log.info("流式语音识别会话已创建，会话ID: {}", sessionId);
            
            return emitter;
            
        } catch (Exception e) {
            log.error("创建流式语音识别会话失败", e);
            SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时
            try {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("创建会话失败: " + e.getMessage()));
                emitter.complete();
            } catch (Exception sendException) {
                log.error("发送错误消息失败", sendException);
            }
            return emitter;
        }
    }
    
    /**
     * 发送音频数据到流式识别会话
     */
    @PostMapping(value = "/streaming/{sessionId}/audio", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<ApiResponse<String>> sendAudioData(
            @PathVariable String sessionId,
            @RequestBody byte[] audioData) {
        
        try {
            log.debug("接收音频数据，会话ID: {}, 数据大小: {} bytes", sessionId, audioData.length);
            
            streamingService.sendAudioData(sessionId, audioData);
            
            return ResponseEntity.ok(ApiResponse.success("音频数据已发送"));
            
        } catch (Exception e) {
            log.error("发送音频数据失败，会话ID: {}", sessionId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("发送音频数据失败: " + e.getMessage()));
        }
    }
    
    /**
     * 停止流式识别
     */
    @PostMapping("/streaming/{sessionId}/stop")
    public ResponseEntity<ApiResponse<String>> stopStreaming(@PathVariable String sessionId) {
        try {
            log.info("停止流式识别，会话ID: {}", sessionId);
            
            streamingService.stopRecognition(sessionId);
            
            return ResponseEntity.ok(ApiResponse.success("识别已停止"));
            
        } catch (Exception e) {
            log.error("停止流式识别失败，会话ID: {}", sessionId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("停止识别失败: " + e.getMessage()));
        }
    }
    
    /**
     * 关闭流式识别会话
     */
    @DeleteMapping("/streaming/{sessionId}")
    public ResponseEntity<ApiResponse<String>> closeSession(@PathVariable String sessionId) {
        try {
            log.info("关闭流式识别会话，会话ID: {}", sessionId);
            
            streamingService.closeSession(sessionId);
            
            return ResponseEntity.ok(ApiResponse.success("会话已关闭"));
            
        } catch (Exception e) {
            log.error("关闭会话失败，会话ID: {}", sessionId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("关闭会话失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取流式识别状态
     */
    @GetMapping("/streaming/status")
    public ResponseEntity<ApiResponse<Integer>> getStreamingStatus() {
        try {
            int activeCount = streamingService.getActiveSessionCount();
            
            return ResponseEntity.ok(ApiResponse.success(activeCount, "当前活跃会话数"));
            
        } catch (Exception e) {
            log.error("获取流式识别状态失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取状态失败: " + e.getMessage()));
        }
    }
    
    /**
     * 从文件名推导音频格式
     */
    private String getFormatFromFilename(String filename) {
        if (filename == null) {
            return "wav";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1).toLowerCase();
        }
        
        return "wav";
    }
}
