package com.xueyifang.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 文件存储配置属性
 *
 * @author xueyifang
 */
@Data
@Component
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageProperties {

    /**
     * 存储类型：local / oss
     */
    private String type = "local";

    /**
     * 本地存储配置
     */
    private LocalStorageConfig local;

    /**
     * 阿里云OSS配置
     */
    private OssStorageConfig oss;

    /**
     * 文件大小限制
     */
    private Map<String, Long> maxSize;

    /**
     * 允许的文件类型
     */
    private List<String> allowedTypes;

    /**
     * 本地存储配置
     */
    @Data
    public static class LocalStorageConfig {
        /**
         * 上传目录（支持绝对路径和相对路径）
         * Windows: D:/xueyifang/uploads 或 D:\xueyifang\\uploads
         * Linux: /var/xueyifang/uploads
         * 相对路径: uploads (相对于项目根目录)
         */
        private String uploadPath = "uploads";

        /**
         * 访问URL前缀
         */
        private String urlPrefix;

        /**
         * 是否启用日期分级（年月）
         */
        private boolean enableDatePath = true;
    }

    /**
     * 阿里云OSS配置
     */
    @Data
    public static class OssStorageConfig {
        /**
         * 节点地址
         */
        private String endpoint;

        /**
         * AccessKey ID
         */
        private String accessKeyId;

        /**
         * AccessKey Secret
         */
        private String accessKeySecret;

        /**
         * Bucket名称
         */
        private String bucketName;

        /**
         * OSS访问域名前缀（可选）
         */
        private String urlPrefix;
    }
}
