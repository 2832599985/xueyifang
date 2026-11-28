package com.xueyifang.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * 跨域配置
 * 
 * @author xueyifang
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${xueyifang.security.cors-allowed-origins:http://localhost:3000,http://127.0.0.1:3000}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] patterns = Arrays.stream(StringUtils.defaultIfBlank(allowedOrigins, "http://localhost:3000").split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .toArray(String[]::new);
        // 允许跨域的路径
        registry.addMapping("/**")
                // 允许的域名（由配置控制，避免生产环境使用通配符 + allowCredentials）
                .allowedOriginPatterns(patterns)
                // 是否允许cookie
                .allowCredentials(true)
                // 允许的请求方式
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // 允许的header
                .allowedHeaders("*")
                // 跨域允许时间
                .maxAge(3600);
    }
}

