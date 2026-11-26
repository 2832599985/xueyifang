package com.xueyifang.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * 阿里云OSS客户端配置
 *
 * @author xueyifang
 */
@Configuration
@ConditionalOnProperty(name = "file.storage.type", havingValue = "aliyun-legacy-oss")
@Slf4j
public class OssClientConfig {

    @Resource
    private FileStorageProperties properties;

    /**
     * 创建OSS客户端
     *
     * @return OSS客户端
     */
    @Bean
    public OSS ossClient() {
        FileStorageProperties.OssStorageConfig config = properties.getOss();
        log.info("初始化阿里云OSS客户端，Endpoint: {}, Bucket: {}", config.getEndpoint(), config.getBucketName());
        return new OSSClientBuilder().build(
                config.getEndpoint(),
                config.getAccessKeyId(),
                config.getAccessKeySecret()
        );
    }
}
