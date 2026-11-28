package com.xueyifang.config;

import com.xueyifang.interceptor.JwtAuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private JwtAuthInterceptor jwtAuthInterceptor;

    @Value("${xueyifang.security.expose-docs-public:false}")
    private boolean exposeDocsPublic;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> excludes = new ArrayList<>(Arrays.asList(
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
                "/favicon.ico",
                "/health",
                "/error",
                "/static/**",
                "/public/**",
                "/ws",
                "/sys-config/register-status"
        ));
        if (exposeDocsPublic) {
            excludes.addAll(Arrays.asList(
                    "/doc.html",
                    "/swagger-resources/**",
                    "/webjars/**",
                    "/v2/api-docs/**",
                    "/swagger-ui.html",
                    "/actuator/**"
            ));
        }
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(excludes);
    }
}
