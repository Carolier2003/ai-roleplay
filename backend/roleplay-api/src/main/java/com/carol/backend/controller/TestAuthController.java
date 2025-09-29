package com.carol.backend.controller;

import com.carol.backend.util.JwtUtil;
import com.carol.backend.util.PasswordEncoderUtil;
import com.carol.backend.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试认证功能的控制器
 * 用于验证 JWT 认证系统是否正常工作
 * 
 * @author carol
 */
@Slf4j
@RestController
@RequestMapping("/api/test-auth")
@RequiredArgsConstructor
public class TestAuthController {

    private final JwtUtil jwtUtil;
    private final PasswordEncoderUtil passwordEncoderUtil;

    /**
     * 健康检查接口（无需认证）
     */
    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        log.info("[healthCheck] 健康检查请求");
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "Auth Test Service is running!");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * 生成测试 Token（无需认证）
     */
    @PostMapping("/generate-token")
    public Map<String, Object> generateToken(@RequestParam String userAccount, @RequestParam Long userId) {
        log.info("[generateToken] 生成测试Token: userAccount={}, userId={}", userAccount, userId);
        
        Map<String, Object> result = new HashMap<>();
        try {
            String accessToken = jwtUtil.generateAccessToken(userAccount, userId);
            String refreshToken = jwtUtil.generateRefreshToken(userAccount, userId);
            
            result.put("success", true);
            result.put("accessToken", accessToken);
            result.put("refreshToken", refreshToken);
            result.put("userAccount", userAccount);
            result.put("userId", userId);
            
            log.info("[generateToken] Token生成成功: userAccount={}, userId={}", userAccount, userId);
        } catch (Exception e) {
            log.error("[generateToken] Token生成失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 验证 Token（无需认证）
     */
    @PostMapping("/validate-token")
    public Map<String, Object> validateToken(@RequestParam String token) {
        log.info("[validateToken] 验证Token");
        
        Map<String, Object> result = new HashMap<>();
        try {
            boolean isValid = jwtUtil.validateToken(token);
            result.put("isValid", isValid);
            
            if (isValid) {
                result.put("userAccount", jwtUtil.getUserAccountFromToken(token));
                result.put("userId", jwtUtil.getUserIdFromToken(token));
                result.put("tokenType", jwtUtil.getTokenTypeFromToken(token));
                log.info("[validateToken] Token验证成功");
            } else {
                log.warn("[validateToken] Token验证失败");
            }
        } catch (Exception e) {
            log.error("[validateToken] Token验证异常: {}", e.getMessage(), e);
            result.put("isValid", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 测试密码加密（无需认证）
     */
    @PostMapping("/test-password")
    public Map<String, Object> testPassword(@RequestParam String password) {
        log.info("[testPassword] 测试密码加密");
        
        Map<String, Object> result = new HashMap<>();
        try {
            String encodedPassword = passwordEncoderUtil.encode(password);
            boolean matches = passwordEncoderUtil.matches(password, encodedPassword);
            
            result.put("success", true);
            result.put("rawPassword", password);
            result.put("encodedPassword", encodedPassword);
            result.put("matches", matches);
            
            log.info("[testPassword] 密码加密测试成功");
        } catch (Exception e) {
            log.error("[testPassword] 密码加密测试失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 测试特定哈希验证（无需认证）
     */
    @PostMapping("/test-hash-verify")
    public Map<String, Object> testHashVerify(@RequestParam String password, @RequestParam String hash) {
        log.info("[testHashVerify] 测试特定哈希验证: password={}, hash={}", password, hash);
        
        Map<String, Object> result = new HashMap<>();
        try {
            boolean matches = passwordEncoderUtil.matches(password, hash);
            
            result.put("success", true);
            result.put("password", password);
            result.put("hash", hash);
            result.put("matches", matches);
            
            log.info("[testHashVerify] 哈希验证测试完成: matches={}", matches);
        } catch (Exception e) {
            log.error("[testHashVerify] 哈希验证测试失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 测试用户上下文（需要认证）
     */
    @GetMapping("/test-context")
    public Map<String, Object> testUserContext() {
        log.info("[testUserContext] 测试用户上下文");
        
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = UserContext.getCurrentUserId();
            String userAccount = UserContext.getCurrentUserAccount();
            
            result.put("success", true);
            result.put("userId", userId);
            result.put("userAccount", userAccount);
            result.put("authenticated", userId != null);
            
            log.info("[testUserContext] 用户上下文测试成功: userId={}, userAccount={}", userId, userAccount);
        } catch (Exception e) {
            log.error("[testUserContext] 用户上下文测试失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 测试数据库连接（无需认证）
     */
    @GetMapping("/test-db")
    public Map<String, Object> testDatabase() {
        log.info("[testDatabase] 测试数据库连接");
        
        Map<String, Object> result = new HashMap<>();
        try {
            // 简单的数据库查询测试
            result.put("success", true);
            result.put("message", "数据库连接测试 - 此接口仅测试基本功能");
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("[testDatabase] 数据库连接测试完成");
        } catch (Exception e) {
            log.error("[testDatabase] 数据库连接测试失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 受保护的测试接口（需要认证）
     */
    @GetMapping("/protected")
    public Map<String, Object> protectedEndpoint() {
        log.info("[protectedEndpoint] 访问受保护的接口");
        
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = UserContext.getCurrentUserId();
            String userAccount = UserContext.getCurrentUserAccount();
            
            if (userId == null) {
                result.put("message", "用户未认证，无法访问受保护资源");
                result.put("authenticated", false);
                log.warn("[protectedEndpoint] 用户未认证");
            } else {
                result.put("message", "成功访问受保护资源");
                result.put("authenticated", true);
                result.put("userId", userId);
                result.put("userAccount", userAccount);
                log.info("[protectedEndpoint] 成功访问受保护资源: userId={}, userAccount={}", userId, userAccount);
            }
        } catch (Exception e) {
            log.error("[protectedEndpoint] 访问受保护资源失败: {}", e.getMessage(), e);
            result.put("message", "访问受保护资源失败: " + e.getMessage());
            result.put("authenticated", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
}
