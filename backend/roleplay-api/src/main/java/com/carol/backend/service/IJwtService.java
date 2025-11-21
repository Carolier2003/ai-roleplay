package com.carol.backend.service;

/**
 * JWT令牌服务接口
 * 
 * @author jianjl
 * @version 1.0
 * @description JWT令牌生成和验证服务
 * @date 2025-01-15
 */
public interface IJwtService {
    
    /**
     * 生成访问令牌
     * 
     * @param userAccount 用户账号
     * @param userId 用户ID
     * @return 访问令牌
     */
    String generateAccessToken(String userAccount, Long userId);
    
    /**
     * 生成刷新令牌
     * 
     * @param userAccount 用户账号
     * @param userId 用户ID
     * @return 刷新令牌
     */
    String generateRefreshToken(String userAccount, Long userId);
    
    /**
     * 从令牌中提取用户名
     * 
     * @param token 令牌
     * @return 用户名
     */
    String extractUsername(String token);
    
    /**
     * 从令牌中提取用户ID
     * 
     * @param token 令牌
     * @return 用户ID
     */
    Long extractUserId(String token);
    
    /**
     * 从令牌中提取令牌类型
     * 
     * @param token 令牌
     * @return 令牌类型
     */
    String extractTokenType(String token);
    
    /**
     * 验证令牌是否有效
     * 
     * @param token 令牌
     * @param userAccount 用户账号
     * @return 是否有效
     */
    boolean isTokenValid(String token, String userAccount);
    
    /**
     * 验证令牌有效性
     * 
     * @param token JWT token
     * @return 是否有效
     */
    boolean validateToken(String token);
    
    /**
     * 从令牌中解析用户账号
     * 
     * @param token JWT token
     * @return 用户账号
     */
    String getUserAccountFromToken(String token);
    
    /**
     * 从令牌中解析令牌类型
     * 
     * @param token JWT token
     * @return 令牌类型 (access/refresh)
     */
    String getTokenTypeFromToken(String token);
}
