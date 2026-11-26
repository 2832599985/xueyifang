package com.xueyifang.utils;

import cn.hutool.crypto.digest.BCrypt;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * 生产级密码哈希工具类（BCrypt 随机盐哈希 + 历史 MD5 平滑校验与自动迁移）
 */
public final class PasswordUtils {

    private PasswordUtils() {
    }

    /**
     * 使用 BCrypt 生成带随机盐的密码哈希
     */
    public static String hashPassword(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(10));
    }

    /**
     * 判断存储的密码哈希是否已升级为 BCrypt 格式
     */
    public static boolean isBcryptHash(String storedHash) {
        return StringUtils.isNotBlank(storedHash)
                && (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$"));
    }

    /**
     * 校验明文密码是否匹配（同时兼容新 BCrypt 哈希与历史固定盐 MD5）
     */
    public static boolean matches(String rawPassword, String storedHash, String legacySalt) {
        if (StringUtils.isAnyBlank(rawPassword, storedHash)) {
            return false;
        }
        if (isBcryptHash(storedHash)) {
            try {
                return BCrypt.checkpw(rawPassword, storedHash);
            } catch (Exception e) {
                return false;
            }
        }
        String salt = StringUtils.isNotBlank(legacySalt) ? legacySalt : "xueyifang";
        String legacyMd5 = DigestUtils.md5DigestAsHex((salt + rawPassword).getBytes(StandardCharsets.UTF_8));
        return legacyMd5.equalsIgnoreCase(storedHash);
    }
}
