package com.carol.backend.service;

import com.alibaba.dashscope.exception.NoApiKeyException;
import com.carol.backend.dto.SpeechRecognitionRequest;
import com.carol.backend.dto.SpeechRecognitionResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * 语音识别服务接口
 * 
 * @author jianjl
 * @version 1.0
 * @description 同步语音识别服务
 * @date 2025-01-15
 */
public interface ISpeechRecognitionService {
    
    /**
     * 同步识别音频文件
     * 
     * @param audioFile 音频文件
     * @param request 识别请求
     * @return 识别响应
     * @throws IOException IO异常
     * @throws NoApiKeyException API密钥异常
     */
    SpeechRecognitionResponse recognizeFile(MultipartFile audioFile, SpeechRecognitionRequest request) 
            throws IOException, NoApiKeyException;
    
    /**
     * 异步识别音频文件，返回CompletableFuture
     * 
     * @param audioFile 音频文件
     * @param request 识别请求
     * @return 识别响应Future
     */
    CompletableFuture<SpeechRecognitionResponse> recognizeFileAsync(MultipartFile audioFile, SpeechRecognitionRequest request);
}
