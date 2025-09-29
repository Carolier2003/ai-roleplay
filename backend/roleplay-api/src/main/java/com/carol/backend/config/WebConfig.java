package com.carol.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 注册JWT认证拦截器和CORS配置
 * 
 * @author carol
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtAuthenticationInterceptor jwtAuthenticationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("[addInterceptors] 注册JWT认证拦截器");
        
        registry.addInterceptor(jwtAuthenticationInterceptor)
                .addPathPatterns("/**") // 拦截所有路径
                .excludePathPatterns(
                    // 静态资源
                    "/static/**",
                    "/public/**",
                    "/*.html",
                    "/*.css",
                    "/*.js",
                    "/*.ico",
                    // 错误页面
                    "/error",
                    // 认证相关接口（注册、登录、刷新令牌等）
                    "/api/auth/**",
                    // 健康检查接口
                    "/api/health",
                    "/health",
                    // 测试接口（不需要JWT验证的部分）
                    "/api/test-auth/health",
                    "/api/test-auth/generate-token",
                    "/api/test-auth/validate-token",
                    "/api/test-auth/test-password",
                    "/api/test-auth/test-hash-verify",
                    "/api/test-auth/test-db",
                    // Swagger文档接口
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        log.info("[addCorsMappings] 配置CORS跨域设置");
        
        registry.addMapping("/**")
                .allowedOriginPatterns("*") // 允许所有域名
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // 预检请求缓存时间
    }
}
