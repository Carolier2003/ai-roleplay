package com.carol.backend.util;

import lombok.extern.slf4j.Slf4j;

/**
 * 安全工具类
 * 用于获取当前登录用户信息，适配新的JWT认证方式
 */
@Slf4j
public class SecurityUtils {

    /**
     * 获取当前登录用户ID
     * 
     * @return 用户ID，如果未登录则返回null
     */
    public static Long getCurrentUserId() {
        try {
            Long userId = UserContext.getCurrentUserId();
            log.debug("[getCurrentUserId] 获取到用户ID: {}", userId);
            return userId;
        } catch (Exception e) {
            log.error("[getCurrentUserId] 获取当前用户ID异常: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取当前登录用户账号
     * 
     * @return 用户账号，如果未登录则返回null
     */
    public static String getCurrentUserAccount() {
        try {
            String userAccount = UserContext.getCurrentUserAccount();
            log.debug("[getCurrentUserAccount] 获取到用户账号: {}", userAccount);
            return userAccount;
        } catch (Exception e) {
            log.error("[getCurrentUserAccount] 获取当前用户账号异常: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取当前用户IP地址
     * 
     * @return IP地址，如果未获取到则返回null
     */
    public static String getCurrentIpAddress() {
        try {
            String ipAddress = UserContext.getCurrentIpAddress();
            log.debug("[getCurrentIpAddress] 获取到IP地址: {}", ipAddress);
            return ipAddress;
        } catch (Exception e) {
            log.error("[getCurrentIpAddress] 获取当前IP地址异常: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 检查当前用户是否已登录
     * 
     * @return 是否已登录
     */
    public static boolean isAuthenticated() {
        try {
            boolean isLoggedIn = UserContext.isUserLoggedIn();
            log.debug("[isAuthenticated] 用户登录状态: {}", isLoggedIn);
            return isLoggedIn;
        } catch (Exception e) {
            log.error("[isAuthenticated] 检查认证状态异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取用户信息摘要（用于日志）
     * 
     * @return 用户信息字符串
     */
    public static String getUserSummary() {
        try {
            return UserContext.getUserSummary();
        } catch (Exception e) {
            log.error("[getUserSummary] 获取用户摘要异常: {}", e.getMessage(), e);
            return "获取用户信息失败";
        }
    }

    /**
     * 要求用户必须已登录，否则抛出异常
     * 
     * @throws IllegalStateException 如果用户未登录
     */
    public static void requireAuthenticated() {
        if (!isAuthenticated()) {
            throw new IllegalStateException("用户未登录");
        }
    }

    /**
     * 要求用户必须已登录并返回用户ID，否则抛出异常
     * 
     * @return 用户ID
     * @throws IllegalStateException 如果用户未登录
     */
    public static Long requireCurrentUserId() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("用户未登录");
        }
        return userId;
    }

    /**
     * 要求用户必须已登录并返回用户账号，否则抛出异常
     * 
     * @return 用户账号
     * @throws IllegalStateException 如果用户未登录
     */
    public static String requireCurrentUserAccount() {
        String userAccount = getCurrentUserAccount();
        if (userAccount == null) {
            throw new IllegalStateException("用户未登录");
        }
        return userAccount;
    }
}