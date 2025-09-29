package com.carol.backend.exception;

import com.carol.backend.enums.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理应用程序中的各种异常，提供标准化的错误响应格式
 * 
 * @author carol
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 检查是否为流式接口请求
     */
    private boolean isStreamingRequest(WebRequest request) {
        String path = request.getDescription(false);
        return path.contains("/api/chat/stream") || path.contains("/api/speech/streaming");
    }

    /**
     * 为流式接口创建错误响应
     */
    private ResponseEntity<String> createStreamingErrorResponse(String message) {
        String sseErrorMessage = "data: {\"error\": \"" + message + "\"}\n\n" +
                                "data: [DONE]\n\n";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.TEXT_PLAIN)
                .body(sseErrorMessage);
    }

    /**
     * 处理参数验证异常
     * 当请求参数不满足@Valid注解的验证规则时触发
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                    FieldError::getField,
                    FieldError::getDefaultMessage,
                    (existing, replacement) -> existing
                ));
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("参数验证失败")
                .message("请求参数不符合要求")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        log.warn("参数验证失败: {} - 路径: {}", fieldErrors, request.getDescription(false));
        
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    /**
     * 处理业务异常
     * 主要处理BusinessException，使用统一的错误码枚举
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(
            BusinessException ex, WebRequest request) {
        
        log.warn("[handleBusinessException] 业务异常: {} - 错误码: {} - 路径: {}", 
            ex.getMessage(), ex.getCode(), request.getDescription(false));
        
        // 检查是否为流式接口请求
        if (isStreamingRequest(request)) {
            return createStreamingErrorResponse("业务错误: " + ex.getMessage());
        }
        
        // 根据错误码确定HTTP状态码
        HttpStatus status = getHttpStatusByErrorCode(ex.getErrorCode());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(ex.getErrorCode().name())
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }
    
    /**
     * 处理业务逻辑异常（兼容旧代码）
     * 主要处理IllegalArgumentException，通常用于业务规则验证失败
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        log.warn("[handleIllegalArgumentException] 业务逻辑异常: {} - 路径: {}", 
            ex.getMessage(), request.getDescription(false));
        
        // 检查是否为流式接口请求
        if (isStreamingRequest(request)) {
            return createStreamingErrorResponse("业务逻辑错误: " + ex.getMessage());
        }
        
        // 根据异常消息判断是否为认证相关错误
        String message = ex.getMessage();
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String errorType = "业务逻辑错误";
        
        if (message != null && (message.contains("账号不存在") || message.contains("密码错误") || 
                               message.contains("令牌") || message.contains("认证"))) {
            status = HttpStatus.UNAUTHORIZED;
            errorType = "认证失败";
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(errorType)
                .message(message)
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }
    
    /**
     * 根据错误码枚举确定HTTP状态码
     */
    private HttpStatus getHttpStatusByErrorCode(ErrorCode errorCode) {
        int code = errorCode.getCode();
        
        // 成功状态
        if (code == 200) {
            return HttpStatus.OK;
        }
        
        // 认证授权相关错误 (3000-3999)
        if (code >= 3000 && code < 4000) {
            return HttpStatus.UNAUTHORIZED;
        }
        
        // 用户相关错误 (2000-2999)
        if (code >= 2000 && code < 3000) {
            if (code == 2000) { // 用户不存在
                return HttpStatus.NOT_FOUND;
            }
            return HttpStatus.BAD_REQUEST;
        }
        
        // 参数错误
        if (code >= 1000 && code < 2000) {
            if (code == 1002) { // 数据不存在
                return HttpStatus.NOT_FOUND;
            }
            return HttpStatus.BAD_REQUEST;
        }
        
        // 其他业务错误默认为BAD_REQUEST
        return HttpStatus.BAD_REQUEST;
    }

    // Spring Security相关异常处理已移除，使用JWT拦截器处理认证和授权

    /**
     * 处理数据库访问异常
     * 当数据库操作失败时触发
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(
            DataAccessException ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("数据库错误")
                .message("数据库操作失败，请稍后重试")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        log.error("数据库访问异常: {} - 路径: {}", ex.getMessage(), request.getDescription(false), ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * 处理运行时异常
     * 捕获其他未被特定处理的运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        
        log.error("运行时异常: {} - 路径: {}", ex.getMessage(), request.getDescription(false), ex);
        
        // 检查是否为流式接口请求
        if (isStreamingRequest(request)) {
            log.warn("流式接口异常，返回SSE格式错误: {}", ex.getMessage());
            return createStreamingErrorResponse("系统运行时发生错误: " + ex.getMessage());
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("系统错误")
                .message("系统运行时发生错误，请稍后重试")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * 处理所有其他异常
     * 作为最后的异常捕获器，处理所有未被上述方法捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(
            Exception ex, WebRequest request) {
        
        log.error("未知异常: {} - 路径: {}", ex.getMessage(), request.getDescription(false), ex);
        
        // 检查是否为流式接口请求
        if (isStreamingRequest(request)) {
            return createStreamingErrorResponse("系统出现未知错误: " + ex.getMessage());
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("未知错误")
                .message("系统出现未知错误，请联系管理员")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
