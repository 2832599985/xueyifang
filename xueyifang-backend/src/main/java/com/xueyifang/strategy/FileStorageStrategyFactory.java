package com.xueyifang.strategy;

import com.xueyifang.config.FileStorageProperties;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 文件存储策略工厂
 *
 * @author xueyifang
 */
@Component
@Slf4j
public class FileStorageStrategyFactory {

    @Resource
    private FileStorageProperties properties;

    @Autowired(required = false)
    private FileStorageStrategy localStorageStrategy;

    @Autowired(required = false)
    private FileStorageStrategy ossStorageStrategy;

    /**
     * 根据配置获取存储策略
     *
     * @return 存储策略
     */
    public FileStorageStrategy getStrategy() {
        String storageType = properties.getType();
        log.debug("当前存储类型: {}", storageType);

        if ("oss".equalsIgnoreCase(storageType)
                || "s3".equalsIgnoreCase(storageType)
                || "minio".equalsIgnoreCase(storageType)) {
            if (ossStorageStrategy == null) {
                log.error("S3/OSS存储策略未配置，请检查配置文件");
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "S3/OSS存储策略未配置");
            }
            log.debug("使用远程 S3/OSS 对象存储策略");
            return ossStorageStrategy;
        }

        if (localStorageStrategy == null) {
            log.error("本地存储策略未配置");
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "本地存储策略未配置");
        }
        log.info("使用本地存储策略");
        return localStorageStrategy;
    }
}
