package com.xueyifang.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 * 用于管理 Token 黑名单和其他缓存操作
 * 
 * @author xueyifang
 */
@Component
@Slf4j
public class RedisUtils {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * Token黑名单的Redis Key前缀
     */
    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    /**
     * 用户全局 Token 失效水位线前缀（用于修改密码/重置密码/禁用账号后立即吊销所有旧 JWT）
     */
    private static final String TOKEN_INVALID_BEFORE_PREFIX = "token:invalid_before:";

    /**
     * 用户缓存的Redis Key前缀
     */
    private static final String USER_CACHE_PREFIX = "user:cache:";

    // ==================== Token 黑名单管理 ====================

    /**
     * 将Token加入黑名单（使用 SHA-256 摘要作为 Redis Key，避免暴露原始长 JWT 并缩短 Key 体积）
     * 
     * @param token Token字符串
     * @param expireTime 过期时间（秒），应该与Token的剩余有效期一致
     */
    public void addTokenToBlacklist(String token, long expireTime) {
        String key = TOKEN_BLACKLIST_PREFIX + hashTokenKey(token);
        redisTemplate.opsForValue().set(key, "1", expireTime, TimeUnit.SECONDS);
    }

    /**
     * 检查Token是否在黑名单中
     * 
     * @param token Token字符串
     * @return true=在黑名单中（已失效），false=不在黑名单中（有效）
     */
    public boolean isTokenInBlacklist(String token) {
        String key = TOKEN_BLACKLIST_PREFIX + hashTokenKey(token);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 从黑名单移除Token（一般不需要，会自动过期）
     * 
     * @param token Token字符串
     */
    public void removeTokenFromBlacklist(String token) {
        String key = TOKEN_BLACKLIST_PREFIX + hashTokenKey(token);
        redisTemplate.delete(key);
    }

    /**
     * 吊销用户在该时间戳之前签发的所有 JWT Token（保留 8 天覆盖最大 Token 有效期）
     */
    public void markUserTokensInvalidBefore(Long userId, long epochMillis) {
        if (userId == null) {
            return;
        }
        String key = TOKEN_INVALID_BEFORE_PREFIX + userId;
        redisTemplate.opsForValue().set(key, String.valueOf(epochMillis), 8L * 24 * 3600, TimeUnit.SECONDS);
    }

    /**
     * 检查用户 Token 是否早于失效水位线（如改密、管理员重置密码、禁用账号后）
     */
    public boolean isTokenInvalidatedForUser(Long userId, Long tokenIssuedAtMillis) {
        if (userId == null) {
            return false;
        }
        Object raw = redisTemplate.opsForValue().get(TOKEN_INVALID_BEFORE_PREFIX + userId);
        if (raw == null) {
            return false;
        }
        try {
            long invalidBefore = Long.parseLong(raw.toString());
            return tokenIssuedAtMillis == null || tokenIssuedAtMillis < invalidBefore;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String hashTokenKey(String token) {
        if (token == null) {
            return "";
        }
        return cn.hutool.crypto.digest.DigestUtil.sha256Hex(token);
    }

    // ==================== 用户缓存管理 ====================

    /**
     * 缓存用户信息（JSON 序列化）
     * 
     * @param userId 用户ID
     * @param user 用户对象
     * @param expireTime 过期时间（秒）
     */
    public void cacheUser(Long userId, Object user, long expireTime) {
        if (userId == null || user == null) {
            return;
        }
        setJson(USER_CACHE_PREFIX + userId, user, expireTime);
    }

    /**
     * 获取缓存的用户信息
     */
    public <T> T getCachedUser(Long userId, Class<T> clazz) {
        if (userId == null) {
            return null;
        }
        return getJson(USER_CACHE_PREFIX + userId, clazz);
    }

    /**
     * 删除用户缓存
     * 
     * @param userId 用户ID
     */
    public void deleteUserCache(Long userId) {
        String key = USER_CACHE_PREFIX + userId;
        redisTemplate.delete(key);
    }

    // ==================== 通用操作 ====================

    /**
     * 设置值
     * 
     * @param key 键
     * @param value 值
     * @param expireTime 过期时间（秒）
     */
    public void set(String key, Object value, long expireTime) {
        redisTemplate.opsForValue().set(key, value, expireTime, TimeUnit.SECONDS);
    }

    /**
     * 获取值
     * 
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除
     * 
     * @param key 键
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 判断key是否存在
     *
     * @param key 键
     * @return true=存在，false=不存在
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 原子操作：如果key不存在则设置值并返回true，否则返回false
     * 用于防重复提交等场景
     *
     * @param key 键
     * @param value 值
     * @param expireTime 过期时间（秒）
     * @return true=设置成功（key不存在），false=设置失败（key已存在）
     */
    public boolean setIfAbsent(String key, Object value, long expireTime) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(key, value, expireTime, TimeUnit.SECONDS)
        );
    }

    /**
     * 设置过期时间
     *
     * @param key 键
     * @param expireTime 过期时间（秒）
     */
    public void expire(String key, long expireTime) {
        redisTemplate.expire(key, expireTime, TimeUnit.SECONDS);
    }

    // ==================== 分布式锁与计数器 ====================

    private static final DefaultRedisScript<Long> UNLOCK_LUA_SCRIPT;

    static {
        UNLOCK_LUA_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_LUA_SCRIPT.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "return redis.call('del', KEYS[1]) " +
                "else return 0 end"
        );
        UNLOCK_LUA_SCRIPT.setResultType(Long.class);
    }

    /**
     * 尝试获取分布式互斥锁（SET NX EX），用于多实例定时任务防并发重复执行
     */
    public boolean tryLock(String lockKey, String lockToken, long expireSeconds) {
        try {
            return Boolean.TRUE.equals(
                    redisTemplate.opsForValue().setIfAbsent(lockKey, lockToken, expireSeconds, TimeUnit.SECONDS)
            );
        } catch (Exception e) {
            log.warn("获取分布式锁异常, key={}", lockKey, e);
            return false;
        }
    }

    /**
     * 安全释放分布式锁（Lua 原子校验持有者 token，防止误删其他实例的锁）
     */
    public void unlock(String lockKey, String lockToken) {
        try {
            redisTemplate.execute(UNLOCK_LUA_SCRIPT, Collections.singletonList(lockKey), lockToken);
        } catch (Exception e) {
            log.warn("释放分布式锁异常, key={}", lockKey, e);
        }
    }

    /**
     * 原子自增并在首次创建时设置过期时间（用于核销码防暴力枚举等限流计数）
     */
    public long incrementWithExpire(String key, long expireSeconds) {
        Long val = redisTemplate.opsForValue().increment(key);
        long current = val != null ? val : 1L;
        if (current == 1L) {
            redisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);
        }
        return current;
    }

    // ==================== JSON 序列化操作 ====================

    /**
     * 将对象序列化为JSON并存储
     *
     * @param key 键
     * @param value 值（对象）
     * @param expireTime 过期时间（秒）
     */
    public void setJson(String key, Object value, long expireTime) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, expireTime, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.error("Redis setJson 序列化失败, key={}", key, e);
        }
    }

    /**
     * 获取JSON并反序列化为对象
     *
     * @param key 键
     * @param clazz 目标类型
     * @param <T> 泛型
     * @return 对象，如果不存在或反序列化失败返回 null
     */
    public <T> T getJson(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue(value.toString(), clazz);
        } catch (JsonProcessingException e) {
            log.error("Redis getJson 反序列化失败, key={}, class={}", key, clazz.getName(), e);
            return null;
        }
    }

    /**
     * 获取JSON并反序列化为List
     *
     * @param key 键
     * @param elementClass 列表元素类型
     * @param <T> 泛型
     * @return List对象，如果不存在或反序列化失败返回 null
     */
    public <T> List<T> getJsonList(String key, Class<T> elementClass) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        try {
            JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, elementClass);
            return objectMapper.readValue(value.toString(), listType);
        } catch (JsonProcessingException e) {
            log.error("Redis getJsonList 反序列化失败, key={}, elementClass={}", key, elementClass.getName(), e);
            return null;
        }
    }
}

