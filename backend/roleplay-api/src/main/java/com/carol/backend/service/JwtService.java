package com.carol.backend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT令牌服务
 */
@Service
public class JwtService {
    
    @Value("${app.jwt.secret:mySecretKey123456789012345678901234567890}")
    private String secret;
    
    @Value("${app.jwt.access-token-expiration:86400000}") // 24小时
    private Long accessTokenExpiration;
    
    @Value("${app.jwt.refresh-token-expiration:604800000}") // 7天
    private Long refreshTokenExpiration;
    
    /**
     * 生成访问令牌
     */
    public String generateAccessToken(String userAccount, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "access");
        return generateToken(claims, userAccount, accessTokenExpiration);
    }
    
    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(String userAccount, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "refresh");
        return generateToken(claims, userAccount, refreshTokenExpiration);
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
     * 从令牌中提取用户名
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    /**
     * 从令牌中提取用户ID
     */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }
    
    /**
     * 从令牌中提取令牌类型
     */
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }
    
    /**
     * 提取令牌中的特定声明
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * 验证令牌是否有效
     */
    public boolean isTokenValid(String token, String userAccount) {
        final String username = extractUsername(token);
        return (username.equals(userAccount)) && !isTokenExpired(token);
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
