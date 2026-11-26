package com.xueyifang.strategy;

import cn.hutool.core.io.FileUtil;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.config.FileStorageProperties;
import com.xueyifang.enums.FileUploadBizEnum;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.utils.FileUrlResolver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * 标准 S3 / OSS 对象存储策略实现（基于 AWS Signature Version 4 标准协议）
 * <p>
 * 兼容自建 SeaweedFS S3、MinIO、AWS S3、Cloudflare R2、腾讯云 COS 与阿里云 OSS。
 */
@Component("ossStorageStrategy")
@Slf4j
public class OssStorageStrategy implements FileStorageStrategy {

    private static final DateTimeFormatter AMZ_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.US);
    private static final DateTimeFormatter DATE_STAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US);
    private static final String REGION = "us-east-1";
    private static final String SERVICE = "s3";

    @Resource
    private FileStorageProperties properties;

    @Resource
    private FileUrlResolver fileUrlResolver;

    @Override
    public String uploadFile(MultipartFile file, FileUploadBizEnum bizType, Long userId) {
        try {
            String originalFilename = file.getOriginalFilename();
            String suffix = FileUtil.getSuffix(originalFilename);
            String cleanExt = StringUtils.isNotBlank(suffix) ? suffix.toLowerCase(Locale.ROOT) : "png";
            // 生产规范：对象 Key 仅使用 UUID + 校验后的小写扩展名，严禁拼接用户原始文件名
            String uuidFilename = UUID.randomUUID().toString().replace("-", "") + "." + cleanExt;
            String objectKey = buildObjectKey(bizType, userId, uuidFilename);

            byte[] payload = file.getBytes();
            String contentType = resolveContentType(cleanExt, file.getContentType());

            sendS3Request("PUT", objectKey, payload, contentType);

            String publicUrl = fileUrlResolver.toPublicUrl(objectKey);
            log.info("文件上传至远程 S3 对象存储成功: key={}, size={}, url={}", objectKey, payload.length, publicUrl);
            return publicUrl;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("上传文件至远程 S3 对象存储失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "对象存储上传失败: " + e.getMessage());
        }
    }

    @Override
    public boolean deleteFile(String fileUrl) {
        try {
            String objectKey = fileUrlResolver.toRelativeKey(fileUrl);
            if (StringUtils.isBlank(objectKey)) {
                log.warn("无法从 URL 提取 ObjectKey: {}", fileUrl);
                return false;
            }
            sendS3Request("DELETE", objectKey, new byte[0], null);
            log.info("从远程 S3 对象存储删除文件成功: key={}", objectKey);
            return true;
        } catch (Exception e) {
            log.error("从远程 S3 对象存储删除文件失败: url={}", fileUrl, e);
            return false;
        }
    }

    private void sendS3Request(String method, String objectKey, byte[] payload, String contentType) throws Exception {
        FileStorageProperties.OssStorageConfig ossConfig = properties.getOss();
        if (ossConfig == null || StringUtils.isAnyBlank(ossConfig.getEndpoint(), ossConfig.getBucketName())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "对象存储配置不完整");
        }

        String rawEndpoint = ossConfig.getEndpoint().trim().replaceAll("/+$", "");
        if (!rawEndpoint.startsWith("http://") && !rawEndpoint.startsWith("https://")) {
            rawEndpoint = "http://" + rawEndpoint;
        }
        String bucket = ossConfig.getBucketName().trim();
        String canonicalUri = "/" + bucket + "/" + objectKey.replaceAll("^/+", "");
        URL url = new URL(rawEndpoint + canonicalUri);
        URI uri = url.toURI();
        String hostHeader = uri.getPort() > 0 ? uri.getHost() + ":" + uri.getPort() : uri.getHost();

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String amzDate = AMZ_DATE_FORMAT.format(now);
        String dateStamp = DATE_STAMP_FORMAT.format(now);
        String payloadHash = sha256Hex(payload);

        String canonicalHeaders;
        String signedHeaders;
        String cacheControl = "public, max-age=31536000, immutable";
        if ("PUT".equalsIgnoreCase(method) && StringUtils.isNotBlank(contentType)) {
            canonicalHeaders = "cache-control:" + cacheControl + "\n"
                    + "content-type:" + contentType + "\n"
                    + "host:" + hostHeader + "\n"
                    + "x-amz-content-sha256:" + payloadHash + "\n"
                    + "x-amz-date:" + amzDate + "\n";
            signedHeaders = "cache-control;content-type;host;x-amz-content-sha256;x-amz-date";
        } else {
            canonicalHeaders = "host:" + hostHeader + "\n"
                    + "x-amz-content-sha256:" + payloadHash + "\n"
                    + "x-amz-date:" + amzDate + "\n";
            signedHeaders = "host;x-amz-content-sha256;x-amz-date";
        }

        String canonicalRequest = method.toUpperCase(Locale.ROOT) + "\n"
                + canonicalUri + "\n"
                + "\n"
                + canonicalHeaders + "\n"
                + signedHeaders + "\n"
                + payloadHash;

        String credentialScope = dateStamp + "/" + REGION + "/" + SERVICE + "/aws4_request";
        String stringToSign = "AWS4-HMAC-SHA256\n"
                + amzDate + "\n"
                + credentialScope + "\n"
                + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));

        byte[] signingKey = getSignatureKey(ossConfig.getAccessKeySecret(), dateStamp, REGION, SERVICE);
        String signature = bytesToHex(hmacSha256(signingKey, stringToSign));

        String authorizationHeader = "AWS4-HMAC-SHA256 "
                + "Credential=" + ossConfig.getAccessKeyId() + "/" + credentialScope + ", "
                + "SignedHeaders=" + signedHeaders + ", "
                + "Signature=" + signature;

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(15000);
        conn.setRequestMethod(method.toUpperCase(Locale.ROOT));
        conn.setRequestProperty("Host", hostHeader);
        conn.setRequestProperty("x-amz-date", amzDate);
        conn.setRequestProperty("x-amz-content-sha256", payloadHash);
        conn.setRequestProperty("Authorization", authorizationHeader);

        if ("PUT".equalsIgnoreCase(method)) {
            conn.setDoOutput(true);
            conn.setFixedLengthStreamingMode(payload.length);
            if (StringUtils.isNotBlank(contentType)) {
                conn.setRequestProperty("Content-Type", contentType);
            }
            conn.setRequestProperty("Cache-Control", cacheControl);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload);
                os.flush();
            }
        }

        int status = conn.getResponseCode();
        if (status < 200 || status >= 300) {
            String errBody = readStreamQuietly(conn.getErrorStream());
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "S3 响应异常 HTTP " + status + ": " + errBody);
        }
        conn.disconnect();
    }

    private String buildObjectKey(FileUploadBizEnum bizType, Long userId, String filename) {
        String yearMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        return bizType.getValue() + "/" + userId + "/" + yearMonth + "/" + filename;
    }

    private static String resolveContentType(String ext, String fallback) {
        if ("jpg".equals(ext) || "jpeg".equals(ext)) {
            return "image/jpeg";
        }
        if ("png".equals(ext)) {
            return "image/png";
        }
        if ("gif".equals(ext)) {
            return "image/gif";
        }
        if ("webp".equals(ext)) {
            return "image/webp";
        }
        return StringUtils.isNotBlank(fallback) ? fallback : "application/octet-stream";
    }

    private static byte[] hmacSha256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] getSignatureKey(String key, String dateStamp, String regionName, String serviceName) throws Exception {
        byte[] kSecret = ("AWS4" + key).getBytes(StandardCharsets.UTF_8);
        byte[] kDate = hmacSha256(kSecret, dateStamp);
        byte[] kRegion = hmacSha256(kDate, regionName);
        byte[] kService = hmacSha256(kRegion, serviceName);
        return hmacSha256(kService, "aws4_request");
    }

    private static String sha256Hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return bytesToHex(md.digest(data));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private static String readStreamQuietly(InputStream is) {
        if (is == null) {
            return "";
        }
        try (InputStream in = is; ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) != -1) {
                bos.write(buf, 0, len);
            }
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
        }
    }
}
