package com.xueyifang.config;

import com.xueyifang.interceptor.JwtAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private JwtAuthInterceptor jwtAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/auth/register",
                        "/campus/list",
                        "/campus/check-fence",
                        "/service/list",
                        "/service/pricing-benchmark",
                        "/service/*/slots",
                        "/demand/list",
                        "/demand/detail/*",
                        "/group-session/list",
                        "/group-session/detail/*",
                        "/creator/leaderboard",
                        "/creator/*/profile",
                        "/review/service/**",
                        "/service/tags",
                        "/professional/list",
                        "/trade-location/list",
                        "/payment/escrow/webhook/callback",
                        "/file/view/**",
                        "/doc.html",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/v2/api-docs/**",
                        "/swagger-ui.html",
                        "/favicon.ico",
                        "/health",
                        "/actuator/**",
                        "/error",
                        "/static/**",
                        "/public/**",
                        "/ws",
                        "/sys-config/register-status"
                );
    }
}
