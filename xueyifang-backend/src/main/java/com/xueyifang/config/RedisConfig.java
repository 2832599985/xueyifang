package com.xueyifang.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.nio.charset.StandardCharsets;

/**
 * Redis 序列化配置
 * 替换 Spring Boot 默认的 JdkSerializationRedisSerializer（会在 Key/Value 前附带 \xac\xed\x00\x05 二进制头），
 * 统一使用 UTF-8 明文字符串序列化 Key 与 Value（兼容 INCR 原子计数与 JSON 字符串存取）。
 *
 * @author xueyifang
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer(StandardCharsets.UTF_8);
        RedisSerializer<Object> plainStringValueSerializer = new RedisSerializer<Object>() {
            @Override
            public byte[] serialize(Object value) throws SerializationException {
                if (value == null) {
                    return new byte[0];
                }
                return String.valueOf(value).getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public Object deserialize(byte[] bytes) throws SerializationException {
                if (bytes == null || bytes.length == 0) {
                    return null;
                }
                return new String(bytes, StandardCharsets.UTF_8);
            }
        };

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(plainStringValueSerializer);
        template.setHashValueSerializer(plainStringValueSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
