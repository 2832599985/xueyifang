package com.xueyifang.config;

import com.xueyifang.interceptor.JwtAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * Web MVC 配置类
 * 注册拦截器、配置路径规则
 * 
 * @author xueyifang
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private JwtAuthInterceptor jwtAuthInterceptor;

    /**
     * 注册拦截器
     *
     * 注意：由于 application.yml 中配置了 server.servlet.context-path=/api
     * 所以这里的路径不需要再加 /api 前缀
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                // 拦截所有接口（不需要加/api前缀，因为context-path已经是/api）
                .addPathPatterns("/**")
                // 排除不需要认证的接口
                .excludePathPatterns(
                        // 用户认证接口
                        "/auth/login",           // 登录
                        "/auth/register",        // 注册

                        // 公开数据查询接口
                        "/service/list",         // 服务列表（首页、服务大厅）
                        // /service/{id} 服务详情由 JwtAuthInterceptor.isPublicEndpoint() 放行（仅GET）
                        "/review/service/**",    // 服务评价列表（公开）
                        "/service/tags",         // 服务分类列表（公开）
                        "/professional/list",    // 专业列表（公开）
                        "/trade-location/list",  // 交易地点列表（公开）

                        // 文件访问接口（图片等静态资源，不需要认证）
                        "/file/view/**",         // 文件查看

                        // API文档接口
                        "/doc.html",             // Knife4j文档
                        "/swagger-resources/**", // Swagger资源
                        "/webjars/**",           // Swagger静态资源
                        "/v2/api-docs/**",       // Swagger API文档
                        "/swagger-ui.html",      // Swagger UI
                        "/favicon.ico",          // 网站图标

                        // 健康检查接口（可选）
                        "/health",
                        "/actuator/**",

                        // 错误处理端点（Spring Boot默认错误页面）
                        "/error",

                        // 静态资源（如果有）
                        "/static/**",
                        "/public/**",

                        // WebSocket 端点（token通过URL参数传递，由WebSocketHandshakeInterceptor验证）
                        // WebSocket 端点（token通过URL参数传递，由WebSocketHandshakeInterceptor验证）
                        "/ws",

                        // 系统配置公开接口
                        "/sys-config/register-status"
                );
    }

    /**
     * 跨域配置（如果需要，也可以使用CorsConfig）
     */
    // @Override
    // public void addCorsMappings(CorsRegistry registry) {
    //     registry.addMapping("/**")
    //             .allowedOriginPatterns("*")
    //             .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
    //             .allowedHeaders("*")
    //             .allowCredentials(true)
    //             .maxAge(3600);
    // }
}

