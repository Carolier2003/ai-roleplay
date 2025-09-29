package com.carol.backend.config;

import com.carol.backend.util.JwtUtil;
import com.carol.backend.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT认证拦截器
 * 参考szml-demo-main的实现，拦截请求进行JWT认证
 * 
 * @author carol
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestPath = request.getRequestURI();
        String method = request.getMethod();
        
        log.info("[preHandle] 🔍 JWT拦截器处理请求: {} {}", method, requestPath);

        // 1. OPTIONS 请求直接放行（CORS 预检请求）
        if ("OPTIONS".equals(method)) {
            log.info("[preHandle] ✅ OPTIONS请求（CORS预检），直接放行: {}", requestPath);
            return true;
        }

        // 2. 检查是否为白名单路径
        if (isWhiteListPath(requestPath, request)) {
            log.info("[preHandle] ✅ 白名单路径，直接放行: {}", requestPath);
            return true;
        }

        // 3. 获取JWT token
        String token = getJwtToken(request);
        if (!StringUtils.hasText(token)) {
            log.warn("[preHandle] ❌ 缺少JWT token: {}", requestPath);
            sendUnauthorizedResponse(response, "缺少认证令牌");
            return false;
        }

        // 4. 验证JWT token
        if (!jwtUtil.validateToken(token)) {
            log.warn("[preHandle] ❌ JWT token验证失败: {}", requestPath);
            sendUnauthorizedResponse(response, "无效或过期的认证令牌");
            return false;
        }

        // 5. 解析用户信息并设置到上下文
        try {
            String userAccount = jwtUtil.getUserAccountFromToken(token);
            Long userId = jwtUtil.getUserIdFromToken(token);
            String ipAddress = getClientIpAddress(request);

            // 设置用户上下文
            UserContext.setUserInfo(userAccount, userId, ipAddress);

            log.info("[preHandle] ✅ JWT认证成功: userAccount={}, userId={}, path={}", 
                    userAccount, userId, requestPath);
            return true;

        } catch (Exception e) {
            log.error("[preHandle] ❌ 解析JWT token异常: {}", e.getMessage(), e);
            sendUnauthorizedResponse(response, "令牌解析失败");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清除用户上下文，避免内存泄漏
        UserContext.clear();
        log.debug("[afterCompletion] 清除用户上下文完成");
    }

    /**
     * 检查是否为白名单路径（不需要认证）
     */
    private boolean isWhiteListPath(String path, HttpServletRequest request) {
        // 认证相关接口
        if (path.startsWith("/api/auth/register") || 
            path.startsWith("/api/auth/login") || 
            path.startsWith("/api/auth/refresh") ||
            path.startsWith("/api/auth/health") ||
        path.startsWith("api/characters")) {
            return true;
        }
        
        // 健康检查和监控接口
        if (path.startsWith("/actuator/") || 
            path.equals("/health") || 
            path.equals("/error")) {
            return true;
        }
        
        // 语音识别接口（临时开放用于测试）
        if (path.startsWith("/api/speech/")) {
            return true;
        }
        
        // 公开测试接口（仅用于开发测试）
        if (path.startsWith("/api/public/")) {
            return true;
        }
        
        
        // 游客模式支持的接口 - 只有在没有Authorization头时才走白名单
        if (path.startsWith("/api/chat/stream") || 
            path.startsWith("/api/chat/chat-stats") ||
            path.startsWith("/api/chat/history") ||
            path.startsWith("/api/characters")) {
            
            // 检查是否有Authorization头
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                // 有token，走正常JWT验证流程
                log.info("[preHandle] 游客模式接口检测到JWT token，走正常验证流程: {}", path);
                return false;
            }
            // 没有token，走游客模式白名单
            log.info("[preHandle] 游客模式接口无JWT token，走白名单: {}", path);
            return true;
        }
        
        // 静态资源
        if (path.startsWith("/static/") || 
            path.startsWith("/public/") || 
            path.endsWith(".html") || 
            path.endsWith(".css") || 
            path.endsWith(".js") || 
            path.endsWith(".ico")) {
            return true;
        }
        
        return false;
    }

    /**
     * 从请求中获取JWT token
     */
    private String getJwtToken(HttpServletRequest request) {
        // 1. 从Authorization header获取
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // 2. 从请求参数获取（可选）
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam;
        }
        
        return null;
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }
        
        String remoteAddr = request.getRemoteAddr();
        return StringUtils.hasText(remoteAddr) ? remoteAddr : "UNKNOWN";
    }

    /**
     * 发送401未授权响应
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        
        String jsonResponse = String.format(
            "{\"code\":401,\"message\":\"%s\",\"data\":null,\"timestamp\":\"%s\"}", 
            message, 
            java.time.LocalDateTime.now()
        );
        
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}
