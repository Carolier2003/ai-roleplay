package com.carol.backend.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户上下文工具类
 * 参考szml-demo-main的实现，使用ThreadLocal存储当前请求的用户信息
 * 
 * @author carol
 */
@Data
@Slf4j
public class UserContext {
    
    private static final ThreadLocal<String> USER_ACCOUNT = new ThreadLocal<>();
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> IP_ADDRESS = new ThreadLocal<>();

    /**
     * 设置用户信息
     * 
     * @param userAccount 用户账号
     * @param userId 用户ID
     * @param ipAddress IP地址
     */
    public static void setUserInfo(String userAccount, Long userId, String ipAddress) {
        log.debug("[setUserInfo] 设置用户上下文: userAccount={}, userId={}, ip={}", 
                userAccount, userId, ipAddress);
        
        USER_ACCOUNT.set(userAccount);
        USER_ID.set(userId);
        IP_ADDRESS.set(ipAddress != null ? ipAddress : "UNKNOWN");
    }

    /**
     * 获取当前用户账号
     * 
     * @return 用户账号
     */
    public static String getCurrentUserAccount() {
        return USER_ACCOUNT.get();
    }

    /**
     * 获取当前用户ID
     * 
     * @return 用户ID
     */
    public static Long getCurrentUserId() {
        return USER_ID.get();
    }

    /**
     * 获取当前用户IP地址
     * 
     * @return IP地址
     */
    public static String getCurrentIpAddress() {
        return IP_ADDRESS.get();
    }

    /**
     * 检查当前是否有用户登录
     * 
     * @return 是否已登录
     */
    public static boolean isUserLoggedIn() {
        return USER_ID.get() != null && USER_ACCOUNT.get() != null;
    }

    /**
     * 清除当前线程的用户信息
     * 重要：必须在请求结束时调用，避免内存泄漏
     */
    public static void clear() {
        log.debug("[clear] 清除用户上下文");
        
        USER_ACCOUNT.remove();
        USER_ID.remove();
        IP_ADDRESS.remove();
    }

    /**
     * 获取用户信息摘要（用于日志）
     * 
     * @return 用户信息字符串
     */
    public static String getUserSummary() {
        Long userId = getCurrentUserId();
        String userAccount = getCurrentUserAccount();
        String ip = getCurrentIpAddress();
        
        if (userId == null) {
            return "未登录用户";
        }
        
        return String.format("用户[ID=%d, Account=%s, IP=%s]", userId, userAccount, ip);
    }
}
