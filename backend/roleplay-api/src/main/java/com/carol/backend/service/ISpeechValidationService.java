package com.carol.backend.service;

import com.carol.backend.dto.SpeechRecognitionRequest;
import org.springframework.web.multipart.MultipartFile;

/**
 * 语音识别验证服务接口
 * 
 * @author jianjl
 * @version 1.0
 * @description 语音识别验证服务
 * @date 2025-01-15
 */
public interface ISpeechValidationService {
    
    /**
     * 验证音频文件
     * 
     * @param file 音频文件
     * @param request 识别请求
     * @return 验证结果
     */
    ValidationResult validateAudioFile(MultipartFile file, SpeechRecognitionRequest request);
    
    /**
     * 验证结果
     */
    class ValidationResult {
        private boolean valid;
        private String errorMessage;
        
        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
