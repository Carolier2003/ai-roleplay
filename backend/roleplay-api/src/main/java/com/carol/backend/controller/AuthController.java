package com.carol.backend.controller;

import com.carol.backend.dto.ApiResponse;
import com.carol.backend.dto.LoginRequest;
import com.carol.backend.dto.LoginResponse;
import com.carol.backend.dto.RegisterRequest;
import com.carol.backend.dto.UserResponse;
import com.carol.backend.service.IAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final IAuthService authService;
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("[register] 用户注册请求, userAccount={}", request.getUserAccount());
        
        try {
            UserResponse response = authService.register(request);
            log.info("[register] 用户注册成功, userAccount={}", request.getUserAccount());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            log.warn("[register] 用户注册失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("[register] 用户注册异常: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("注册失败"));
        }
    }
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("[login] 用户登录请求, userAccount={}", request.getUserAccount());
        
        try {
            LoginResponse response = authService.login(request);
            log.info("[login] 用户登录成功, userAccount={}", request.getUserAccount());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            log.warn("[login] 用户登录失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("[login] 用户登录异常: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("登录失败"));
        }
    }
    
    /**
     * 刷新令牌
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@RequestBody RefreshTokenRequest request) {
        log.info("[refreshToken] 刷新令牌请求");
        
        try {
            LoginResponse response = authService.refreshToken(request.getRefreshToken());
            log.info("[refreshToken] 刷新令牌成功");
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            log.warn("[refreshToken] 刷新令牌失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("[refreshToken] 刷新令牌异常: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("刷新令牌失败"));
        }
    }
    
    /**
     * 刷新令牌请求DTO
     */
    public static class RefreshTokenRequest {
        private String refreshToken;
        
        public String getRefreshToken() {
            return refreshToken;
        }
        
        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth service is running");
    }
    
    /**
     * 获取当前用户信息（需要认证）
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        log.info("[getCurrentUser] 获取当前用户信息请求");
        
        try {
            UserResponse response = authService.getCurrentUser();
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            log.warn("[getCurrentUser] 获取用户信息失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("[getCurrentUser] 获取用户信息异常: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("获取用户信息失败"));
        }
    }
}
