package com.carol.backend.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 统一错误响应格式
 * 
 * @author carol
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    /**
     * 错误发生时间
     */
    private LocalDateTime timestamp;
    
    /**
     * HTTP状态码
     */
    private Integer status;
    
    /**
     * 错误类型
     */
    private String error;
    
    /**
     * 错误消息
     */
    private String message;
    
    /**
     * 请求路径
     */
    private String path;
    
    /**
     * 详细错误信息（如字段验证错误）
     */
    private Map<String, Object> details;
}
