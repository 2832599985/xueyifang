package com.xueyifang.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置
 * 
 * @author xueyifang
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 允许跨域的路径
        registry.addMapping("/**")
                // 允许的域名
                .allowedOriginPatterns("*")
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

