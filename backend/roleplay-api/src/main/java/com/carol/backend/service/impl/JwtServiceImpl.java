package com.carol.backend.service.impl;

import com.carol.backend.service.IJwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT令牌服务实现类
 * 
 * @author jianjl
 * @version 1.0
 * @description JWT令牌生成和验证服务实现
 * @date 2025-01-15
 */
@Slf4j
@Service
public class JwtServiceImpl implements IJwtService {
    
    @Value("${jwt.secret:ai-roleplay-secret-key-for-jwt-token-generation-and-validation-must-be-at-least-32-characters-long}")
    private String secret;
    
    @Value("${jwt.expiration:86400000}") // 24小时
    private Long accessTokenExpiration;
    
    @Value("${jwt.refresh-expiration:604800000}") // 7天
    private Long refreshTokenExpiration;
    
    @Override
    public String generateAccessToken(String userAccount, Long userId) {
        log.debug("[generateAccessToken] 生成访问令牌: userAccount={}, userId={}", userAccount, userId);
        
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", userId);
            claims.put("type", "access");
            String token = generateToken(claims, userAccount, accessTokenExpiration);
            log.debug("[generateAccessToken] 访问令牌生成成功");
            return token;
        } catch (Exception e) {
            log.error("[generateAccessToken] 生成访问令牌失败: userAccount={}, error={}", userAccount, e.getMessage(), e);
            throw new RuntimeException("生成访问令牌失败", e);
        }
    }
    
    @Override
    public String generateRefreshToken(String userAccount, Long userId) {
        log.debug("[generateRefreshToken] 生成刷新令牌: userAccount={}, userId={}", userAccount, userId);
        
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", userId);
            claims.put("type", "refresh");
            String token = generateToken(claims, userAccount, refreshTokenExpiration);
            log.debug("[generateRefreshToken] 刷新令牌生成成功");
            return token;
        } catch (Exception e) {
            log.error("[generateRefreshToken] 生成刷新令牌失败: userAccount={}, error={}", userAccount, e.getMessage(), e);
            throw new RuntimeException("生成刷新令牌失败", e);
        }
    }
    
    @Override
    public String extractUsername(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (Exception e) {
            log.error("[extractUsername] 提取用户名失败: error={}", e.getMessage(), e);
            throw new RuntimeException("提取用户名失败", e);
        }
    }
    
    @Override
    public Long extractUserId(String token) {
        try {
            return extractClaim(token, claims -> claims.get("userId", Long.class));
        } catch (Exception e) {
            log.error("[extractUserId] 提取用户ID失败: error={}", e.getMessage(), e);
            throw new RuntimeException("提取用户ID失败", e);
        }
    }
    
    @Override
    public String extractTokenType(String token) {
        try {
            return extractClaim(token, claims -> claims.get("type", String.class));
        } catch (Exception e) {
            log.error("[extractTokenType] 提取令牌类型失败: error={}", e.getMessage(), e);
            throw new RuntimeException("提取令牌类型失败", e);
        }
    }
    
    @Override
    public boolean isTokenValid(String token, String userAccount) {
        try {
            final String username = extractUsername(token);
            boolean valid = (username.equals(userAccount)) && !isTokenExpired(token);
            log.debug("[isTokenValid] 令牌验证结果: userAccount={}, valid={}", userAccount, valid);
            return valid;
        } catch (Exception e) {
            log.error("[isTokenValid] 验证令牌失败: userAccount={}, error={}", userAccount, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            log.debug("[validateToken] 令牌验证成功");
            return true;
        } catch (Exception e) {
            log.warn("[validateToken] 令牌验证失败: error={}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getUserAccountFromToken(String token) {
        return extractUsername(token);
    }
    
    @Override
    public String getTokenTypeFromToken(String token) {
        return extractTokenType(token);
    }
    
    /**
     * 生成令牌
     */
    private String generateToken(Map<String, Object> extraClaims, String subject, Long expiration) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }
    
    /**
     * 提取令牌中的特定声明
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * 检查令牌是否过期
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    /**
     * 从令牌中提取过期时间
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    /**
     * 提取令牌中的所有声明
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * 获取签名密钥
     */
    private SecretKey getSignInKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
}
