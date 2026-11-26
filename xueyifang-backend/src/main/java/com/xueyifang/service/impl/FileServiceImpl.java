package com.xueyifang.service.impl;

import cn.hutool.core.io.FileUtil;
import com.xueyifang.config.FileStorageProperties;
import com.xueyifang.enums.FileUploadBizEnum;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.service.FileService;
import com.xueyifang.strategy.FileStorageStrategy;
import com.xueyifang.strategy.FileStorageStrategyFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 文件服务实现类
 *
 * @author xueyifang
 */
@Service
@Slf4j
public class FileServiceImpl implements FileService {

    @Resource
    private FileStorageStrategyFactory strategyFactory;

    @Resource
    private FileStorageProperties properties;

    @Override
    public String uploadFile(MultipartFile file, String biz, Long userId) {
        // 1. 获取业务类型枚举
        FileUploadBizEnum bizEnum = FileUploadBizEnum.getEnumByValue(biz);
        if (bizEnum == null) {
            log.error("不支持的业务类型: {}", biz);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的业务类型");
        }

        // 2. 校验文件
        validateFile(file, bizEnum);

        // 3. 获取存储策略并上传
        FileStorageStrategy strategy = strategyFactory.getStrategy();
        return strategy.uploadFile(file, bizEnum, userId);
    }

    @Override
    public List<String> uploadFiles(List<MultipartFile> files, String biz, Long userId) {
        if (files == null || files.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件列表不能为空");
        }

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            String url = uploadFile(file, biz, userId);
            urls.add(url);
        }

        return urls;
    }

    @Override
    public boolean deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件URL不能为空");
        }

        FileStorageStrategy strategy = strategyFactory.getStrategy();
        return strategy.deleteFile(fileUrl);
    }

    /**
     * 校验文件
     *
     * @param file    文件
     * @param bizType 业务类型
     */
    private void validateFile(MultipartFile file, FileUploadBizEnum bizType) {
        // 1. 检查文件是否为空
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }

        // 2. 检查文件大小
        long fileSize = file.getSize();
        Long maxSize = bizType.getMaxSize();
        if (fileSize > maxSize) {
            long maxSizeMB = maxSize / (1024 * 1024);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 " + maxSizeMB + "MB");
        }

        // 3. 检查文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不能为空");
        }

        String suffix = FileUtil.getSuffix(originalFilename).toLowerCase();
        List<String> allowedTypes = properties.getAllowedTypes();
        if (allowedTypes == null || allowedTypes.isEmpty()) {
            // 默认允许的图片类型
            allowedTypes = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
        }

        if (!allowedTypes.contains(suffix)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的文件类型，仅支持: " + String.join(", ", allowedTypes));
        }

        // 4. 校验文件魔数（Magic Bytes），防止将恶意脚本或非图片文件改后缀伪装上传
        if (!hasValidImageMagicBytes(file)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件内容不是合法的图片格式（魔数校验未通过）");
        }

        log.debug("文件校验通过: filename={}, size={}, type={}", originalFilename, fileSize, suffix);
    }

    private boolean hasValidImageMagicBytes(MultipartFile file) {
        try (java.io.InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            int read = is.read(header);
            if (read < 4) {
                return false;
            }
            // JPEG: FF D8 FF
            if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
                return true;
            }
            // PNG: 89 50 4E 47
            if ((header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
                return true;
            }
            // GIF: 47 49 46 38 ("GIF8")
            if (header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x38) {
                return true;
            }
            // WebP: "RIFF" .... "WEBP"
            if (read >= 12
                    && header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                    && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50) {
                return true;
            }
            return false;
        } catch (Exception e) {
            log.warn("读取文件魔数失败", e);
            return false;
        }
    }
}
