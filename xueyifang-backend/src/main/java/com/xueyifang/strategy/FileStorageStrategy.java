package com.xueyifang.strategy;

import com.xueyifang.enums.FileUploadBizEnum;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储策略接口
 *
 * @author xueyifang
 */
public interface FileStorageStrategy {

    /**
     * 上传文件
     *
     * @param file    文件
     * @param bizType 业务类型
     * @param userId  用户ID
     * @return 文件访问URL
     */
    String uploadFile(MultipartFile file, FileUploadBizEnum bizType, Long userId);

    /**
     * 删除文件
     *
     * @param fileUrl 文件URL
     * @return 是否成功
     */
    boolean deleteFile(String fileUrl);
}
