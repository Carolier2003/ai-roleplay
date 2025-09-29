package com.carol.backend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT工具类
 * 参考szml-demo-main的实现，提供JWT token的生成、验证和解析功能
 * 
 * @author carol
 */
@Slf4j
@Component
public class JwtUtil {
    
    @Value("${jwt.secret:ai-roleplay-secret-key-for-jwt-token-generation-and-validation}")
    private String secret;
    
    @Value("${jwt.expiration:86400000}") // 默认24小时
    private Long expiration;
    
    @Value("${jwt.refresh-expiration:604800000}") // 默认7天
    private Long refreshExpiration;

    /**
     * 生成访问令牌
     * 
     * @param userAccount 用户账号
     * @param userId 用户ID
     * @return JWT token
     */
    public String generateAccessToken(String userAccount, Long userId) {
        log.info("[generateAccessToken] 生成访问令牌: userAccount={}, userId={}", userAccount, userId);
        
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        return Jwts.builder()
                .setSubject(userAccount)
                .claim("type", "access")
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    /**
     * 生成刷新令牌
     * 
     * @param userAccount 用户账号
     * @param userId 用户ID
     * @return JWT refresh token
     */
    public String generateRefreshToken(String userAccount, Long userId) {
        log.info("[generateRefreshToken] 生成刷新令牌: userAccount={}, userId={}", userAccount, userId);
        
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        return Jwts.builder()
                .setSubject(userAccount)
                .claim("type", "refresh")
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(key)
                .compact();
    }

    /**
     * 验证令牌有效性
     * 
     * @param token JWT token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.warn("[validateToken] Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从令牌中解析用户账号
     * 
     * @param token JWT token
     * @return 用户账号
     */
    public String getUserAccountFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    /**
     * 从令牌中解析用户ID
     * 
     * @param token JWT token
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("userId", Long.class);
    }

    /**
     * 从令牌中解析令牌类型
     * 
     * @param token JWT token
     * @return 令牌类型 (access/refresh)
     */
    public String getTokenTypeFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("type", String.class);
    }

    /**
     * 检查令牌是否过期
     * 
     * @param token JWT token
     * @return 是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
