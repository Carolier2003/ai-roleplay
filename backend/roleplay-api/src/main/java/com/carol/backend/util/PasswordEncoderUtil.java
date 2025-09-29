package com.carol.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Component;

/**
 * 密码加密工具类
 * 参考szml-demo-main的实现，使用BCrypt进行密码加密和验证
 * 
 * @author carol
 */
@Slf4j
@Component
public class PasswordEncoderUtil {

    /**
     * 加密用户密码
     * 
     * @param rawPassword 原始密码
     * @return 加密后的密码
     */
    public String encode(String rawPassword) {
        log.info("[encode] 开始加密密码");
        
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        
        // BCrypt.gensalt() 使用默认的复杂度（10），可根据需要调整
        String encodedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt());
        
        log.info("[encode] 密码加密完成");
        return encodedPassword;
    }

    /**
     * 验证密码是否正确
     * 
     * @param rawPassword 用户输入的原始密码
     * @param encodedPassword 数据库中存储的加密密码
     * @return 匹配返回 true，否则 false
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        log.info("[matches] 开始验证密码");
        
        if (rawPassword == null || encodedPassword == null) {
            log.warn("[matches] 密码验证失败: 密码或加密密码为空");
            return false;
        }
        
        try {
            boolean matches = BCrypt.checkpw(rawPassword, encodedPassword);
            log.info("[matches] 密码验证结果: {}", matches ? "成功" : "失败");
            return matches;
        } catch (Exception e) {
            log.error("[matches] 密码验证异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查密码强度（可选功能）
     * 
     * @param password 密码
     * @return 是否符合强度要求
     */
    public boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 6) {
            return false;
        }
        
        // 简单的密码强度检查：至少6位，包含字母和数字
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        
        return hasLetter && hasDigit;
    }
}
