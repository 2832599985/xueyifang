package com.xueyifang.common;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT配置类，读取application.yml中的配置
 *
 * @author xueyifang
 */
@Component
@Data
public class JwtProperties {
    
    /**
     * JWT存储的请求头
     */
    @Value("${jwt.tokenHeader}")
    private String tokenHeader;
    
    /**
     * jwt加解密使用的密钥
     */
    @Value("${jwt.secret}")
    private String secret;
    
    /**
     * JWT的超时时间（毫秒）
     */
    @Value("${jwt.expiration}")
    private Long expiration;
    
    /**
     * JWT负载中的开头（Bearer）
     */
    @Value("${jwt.tokenHead}")
    private String tokenHead;
    
    public JwtProperties() {
    }
    
    public JwtProperties(String tokenHeader, String secret, Long expiration, String tokenHead) {
        this.tokenHeader = tokenHeader;
        this.secret = secret;
        this.expiration = expiration;
        this.tokenHead = tokenHead;
    }
}

