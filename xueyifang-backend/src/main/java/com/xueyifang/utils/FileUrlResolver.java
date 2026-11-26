package com.xueyifang.utils;

import com.xueyifang.config.FileStorageProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 统一文件对象 Key 与公网访问 URL 转换器
 * <p>
 * 数据库中统一仅存储相对 ObjectKey（如 {@code service_image/2/202611/8f9a1b2c.png}），
 * 读取出库时动态拼接当前生效的对象存储/CDN 域名前缀，实现存储域名无感迁移。
 */
@Component
public class FileUrlResolver {

    private static final String LOCAL_VIEW_PREFIX = "/api/file/view/";

    @Resource
    private FileStorageProperties properties;

    /**
     * 将数据库存储的相对 ObjectKey（或历史遗留路径）解析为前端可直接访问的完整 URL
     */
    public String toPublicUrl(String storedPath) {
        if (StringUtils.isBlank(storedPath)) {
            return storedPath;
        }
        String clean = storedPath.trim();
        if (clean.startsWith("http://") || clean.startsWith("https://") || clean.startsWith("data:")) {
            return clean;
        }

        String objectKey = toRelativeKey(clean);
        String prefix = getActiveUrlPrefix();
        if (StringUtils.isBlank(prefix)) {
            return LOCAL_VIEW_PREFIX + objectKey;
        }
        String normalizedPrefix = prefix.endsWith("/") ? prefix : prefix + "/";
        return normalizedPrefix + objectKey;
    }

    /**
     * 将完整访问 URL 还原为相对 ObjectKey，供入库或删除对象使用
     */
    public String toRelativeKey(String fileUrl) {
        if (StringUtils.isBlank(fileUrl)) {
            return fileUrl;
        }
        String clean = fileUrl.trim().replace("\\", "/");

        if (clean.startsWith(LOCAL_VIEW_PREFIX)) {
            clean = clean.substring(LOCAL_VIEW_PREFIX.length());
        } else if (clean.startsWith("/file/view/")) {
            clean = clean.substring("/file/view/".length());
        }

        if (properties != null) {
            if (properties.getOss() != null) {
                String ossPrefix = properties.getOss().getUrlPrefix();
                clean = stripPrefix(clean, ossPrefix);
                String endpoint = properties.getOss().getEndpoint();
                String bucket = properties.getOss().getBucketName();
                if (StringUtils.isNoneBlank(endpoint, bucket)) {
                    String normEndpoint = endpoint.replaceFirst("^https?://", "").replaceAll("/+$", "");
                    clean = stripPrefix(clean, "http://" + normEndpoint + "/" + bucket);
                    clean = stripPrefix(clean, "https://" + normEndpoint + "/" + bucket);
                    clean = stripPrefix(clean, "https://" + bucket + "." + normEndpoint);
                    clean = stripPrefix(clean, "http://" + bucket + "." + normEndpoint);
                }
            }
            if (properties.getLocal() != null) {
                clean = stripPrefix(clean, properties.getLocal().getUrlPrefix());
            }
        }

        while (clean.startsWith("/")) {
            clean = clean.substring(1);
        }
        return clean;
    }

    private String getActiveUrlPrefix() {
        if (properties == null) {
            return LOCAL_VIEW_PREFIX;
        }
        String type = properties.getType();
        if (("oss".equalsIgnoreCase(type) || "s3".equalsIgnoreCase(type) || "minio".equalsIgnoreCase(type))
                && properties.getOss() != null) {
            FileStorageProperties.OssStorageConfig oss = properties.getOss();
            if (StringUtils.isNotBlank(oss.getUrlPrefix())) {
                return oss.getUrlPrefix().trim();
            }
            if (StringUtils.isNoneBlank(oss.getEndpoint(), oss.getBucketName())) {
                String ep = oss.getEndpoint().trim().replaceAll("/+$", "");
                if (!ep.startsWith("http://") && !ep.startsWith("https://")) {
                    ep = "https://" + ep;
                }
                return ep + "/" + oss.getBucketName().trim();
            }
        }
        if (properties.getLocal() != null && StringUtils.isNotBlank(properties.getLocal().getUrlPrefix())) {
            return properties.getLocal().getUrlPrefix().trim();
        }
        return LOCAL_VIEW_PREFIX;
    }

    private static String stripPrefix(String input, String prefix) {
        if (StringUtils.isAnyBlank(input, prefix)) {
            return input;
        }
        String norm = prefix.trim().replaceAll("/+$", "");
        if (input.startsWith(norm + "/")) {
            return input.substring(norm.length() + 1);
        }
        return input;
    }
}
