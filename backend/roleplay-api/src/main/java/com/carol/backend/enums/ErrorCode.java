package com.carol.backend.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举
 * 统一管理系统中的所有错误码和错误信息
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {
    
    // 通用错误码 (1000-1999)
    SUCCESS(200, "操作成功"),
    SYSTEM_ERROR(1000, "系统内部错误"),
    PARAM_ERROR(1001, "参数错误"),
    DATA_NOT_FOUND(1002, "数据不存在"),
    OPERATION_FAILED(1003, "操作失败"),
    
    // 用户相关错误码 (2000-2999)
    USER_NOT_FOUND(2000, "用户不存在"),
    USER_ACCOUNT_EXISTS(2001, "用户账号已存在"),
    USER_EMAIL_EXISTS(2002, "邮箱已被使用"),
    USER_PASSWORD_ERROR(2003, "密码错误"),
    USER_ACCOUNT_DISABLED(2004, "用户账号已被禁用"),
    USER_LOGIN_EXPIRED(2005, "登录已过期"),
    
    // 认证授权相关错误码 (3000-3999)
    AUTH_FAILED(3000, "认证失败"),
    TOKEN_INVALID(3001, "令牌无效"),
    TOKEN_EXPIRED(3002, "令牌已过期"),
    PERMISSION_DENIED(3003, "权限不足"),
    LOGIN_REQUIRED(3004, "请先登录"),
    
    // 个人资料相关错误码 (4000-4099)
    PROFILE_UPDATE_FAILED(4000, "个人资料更新失败"),
    PROFILE_EMAIL_TAKEN(4001, "该邮箱已被其他用户使用"),
    PROFILE_INVALID_AVATAR_URL(4002, "头像URL格式不正确"),
    PROFILE_INVALID_PHONE(4003, "手机号格式不正确"),
    PROFILE_INVALID_BIRTHDAY(4004, "生日格式不正确"),
    
    // 聊天相关错误码 (5000-5999)
    CHAT_CHARACTER_NOT_FOUND(5000, "角色不存在"),
    CHAT_MESSAGE_EMPTY(5001, "消息内容不能为空"),
    CHAT_HISTORY_LOAD_FAILED(5002, "聊天记录加载失败"),
    CHAT_SEND_FAILED(5003, "消息发送失败"),
    
    // 语音识别相关错误码 (6000-6999)
    SPEECH_FILE_EMPTY(6000, "音频文件为空"),
    SPEECH_FILE_TOO_LARGE(6001, "音频文件过大"),
    SPEECH_FORMAT_UNSUPPORTED(6002, "不支持的音频格式"),
    SPEECH_RECOGNITION_FAILED(6003, "语音识别失败"),
    SPEECH_SERVICE_UNAVAILABLE(6004, "语音识别服务不可用"),
    
    // 文件相关错误码 (7000-7999)
    FILE_UPLOAD_FAILED(7000, "文件上传失败"),
    FILE_NOT_FOUND(7001, "文件不存在"),
    FILE_SIZE_EXCEEDED(7002, "文件大小超出限制"),
    FILE_TYPE_NOT_SUPPORTED(7003, "不支持的文件类型");
    
    /**
     * 错误码
     */
    private final int code;
    
    /**
     * 错误信息
     */
    private final String message;
    
    /**
     * 判断是否为成功状态
     */
    public boolean isSuccess() {
        return this.code == SUCCESS.code;
    }
    
    /**
     * 判断是否为错误状态
     */
    public boolean isError() {
        return !isSuccess();
    }
}
