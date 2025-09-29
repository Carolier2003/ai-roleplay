package com.carol.backend.exception;

import com.carol.backend.enums.ErrorCode;
import lombok.Getter;

/**
 * 业务异常类
 * 用于处理业务逻辑中的异常情况
 */
@Getter
public class BusinessException extends RuntimeException {
    
    /**
     * 错误码
     */
    private final ErrorCode errorCode;
    
    /**
     * 错误数据（可选）
     */
    private final Object data;
    
    /**
     * 构造函数 - 使用错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.data = null;
    }
    
    /**
     * 构造函数 - 使用错误码枚举和自定义消息
     */
    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.data = null;
    }
    
    /**
     * 构造函数 - 使用错误码枚举和错误数据
     */
    public BusinessException(ErrorCode errorCode, Object data) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.data = data;
    }
    
    /**
     * 构造函数 - 使用错误码枚举、自定义消息和错误数据
     */
    public BusinessException(ErrorCode errorCode, String customMessage, Object data) {
        super(customMessage);
        this.errorCode = errorCode;
        this.data = data;
    }
    
    /**
     * 构造函数 - 使用错误码枚举和原因异常
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.data = null;
    }
    
    /**
     * 构造函数 - 使用错误码枚举、自定义消息和原因异常
     */
    public BusinessException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.errorCode = errorCode;
        this.data = null;
    }
    
    /**
     * 获取错误码数值
     */
    public int getCode() {
        return errorCode.getCode();
    }
    
    /**
     * 获取错误码枚举的默认消息
     */
    public String getDefaultMessage() {
        return errorCode.getMessage();
    }
    
    /**
     * 静态工厂方法 - 创建业务异常
     */
    public static BusinessException of(ErrorCode errorCode) {
        return new BusinessException(errorCode);
    }
    
    /**
     * 静态工厂方法 - 创建带自定义消息的业务异常
     */
    public static BusinessException of(ErrorCode errorCode, String customMessage) {
        return new BusinessException(errorCode, customMessage);
    }
    
    /**
     * 静态工厂方法 - 创建带错误数据的业务异常
     */
    public static BusinessException of(ErrorCode errorCode, Object data) {
        return new BusinessException(errorCode, data);
    }
    
    /**
     * 静态工厂方法 - 创建带自定义消息和错误数据的业务异常
     */
    public static BusinessException of(ErrorCode errorCode, String customMessage, Object data) {
        return new BusinessException(errorCode, customMessage, data);
    }
}
