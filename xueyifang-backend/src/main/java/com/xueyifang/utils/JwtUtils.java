package com.xueyifang.utils;

import com.xueyifang.common.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类（增强版，模仿 JwtKit）
 * 
 * @author xueyifang
 */
@Component
public class JwtUtils {

    @Resource
    private JwtProperties jwtProperties;

    /**
     * 生成Token（支持泛型，可传入任何用户对象）
     *
     * @param user 用户对象
     * @return JWT Token字符串
     */
    public <T> String generateToken(T user) {
        Map<String, Object> claims = new HashMap<>(10);
        claims.put("user", user.toString());
        claims.put("createDate", new Date());
        claims.put("timestamp", System.currentTimeMillis());
        
        return Jwts.builder()
                .addClaims(claims)
                // 过期时间
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration()))
                // 加密算法和密钥（使用HS256）
                .signWith(SignatureAlgorithm.HS256, jwtProperties.getSecret())
                .compact();
    }

    /**
     * 生成Token（基于用户ID）
     *
     * @param userId 用户ID
     * @return JWT Token字符串
     */
    public String generateToken(Long userId) {
        Map<String, Object> claims = new HashMap<>(10);
        claims.put("userId", userId);
        claims.put("createDate", new Date());
        claims.put("timestamp", System.currentTimeMillis());
        
        return Jwts.builder()
                .addClaims(claims)
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration()))
                .signWith(SignatureAlgorithm.HS256, jwtProperties.getSecret())
                .compact();
    }

    /**
     * 解析Token，获取Claims
     *
     * @param token 要解析的Token
     * @return Claims对象（包含用户信息、过期时间等）
     */
    public Claims parseJwtToken(String token) {
        return Jwts.parser()
                .setSigningKey(jwtProperties.getSecret())
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 从Token中获取用户ID
     *
     * @param token JWT Token
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseJwtToken(token);
        // 先尝试从 subject 获取
        if (claims.getSubject() != null) {
            return Long.parseLong(claims.getSubject());
        }
        // 否则从 claims 中获取
        Object userId = claims.get("userId");
        if (userId != null) {
            return Long.parseLong(userId.toString());
        }
        return null;
    }

    /**
     * 获取 Token 签发时间戳（毫秒），用于改密/禁用账号后的全局旧 Token 吊销校验
     */
    public Long getTokenIssuedAtMillis(String token) {
        try {
            Claims claims = parseJwtToken(token);
            Object ts = claims.get("timestamp");
            if (ts instanceof Number) {
                return ((Number) ts).longValue();
            }
            if (ts != null) {
                return Long.parseLong(ts.toString());
            }
            Date iat = claims.getIssuedAt();
            return iat != null ? iat.getTime() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 验证Token是否合法
     *
     * @param token JWT Token
     * @return true=合法，false=非法或过期
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(jwtProperties.getSecret())
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断Token是否过期
     *
     * @param token JWT Token
     * @return true=已过期，false=未过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseJwtToken(token);
            Date expiration = claims.getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 刷新Token（生成新的Token）
     *
     * @param token 旧Token
     * @return 新Token
     */
    public String refreshToken(String token) {
        Claims claims = parseJwtToken(token);
        claims.put("createDate", new Date());
        
        return Jwts.builder()
                .setClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration()))
                .signWith(SignatureAlgorithm.HS256, jwtProperties.getSecret())
                .compact();
    }
}

