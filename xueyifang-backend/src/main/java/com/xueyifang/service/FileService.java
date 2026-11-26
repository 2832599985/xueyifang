package com.xueyifang.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件服务接口
 *
 * @author xueyifang
 */
public interface FileService {

    /**
     * 上传单个文件
     *
     * @param file   文件
     * @param biz    业务类型
     * @param userId 用户ID
     * @return 文件访问URL
     */
    String uploadFile(MultipartFile file, String biz, Long userId);

    /**
     * 批量上传文件
     *
     * @param files  文件列表
     * @param biz    业务类型
     * @param userId 用户ID
     * @return 文件访问URL列表
     */
    List<String> uploadFiles(List<MultipartFile> files, String biz, Long userId);

    /**
     * 删除文件
     *
     * @param fileUrl 文件URL
     * @return 是否成功
     */
    boolean deleteFile(String fileUrl);
}
