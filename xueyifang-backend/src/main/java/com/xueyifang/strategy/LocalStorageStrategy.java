package com.xueyifang.strategy;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import com.xueyifang.config.FileStorageProperties;
import com.xueyifang.enums.FileUploadBizEnum;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 本地存储策略实现
 *
 * @author xueyifang
 */
@Component("localStorageStrategy")
@Slf4j
public class LocalStorageStrategy implements FileStorageStrategy {

    @Resource
    private FileStorageProperties properties;

    @Override
    public String uploadFile(MultipartFile file, FileUploadBizEnum bizType, Long userId) {
        try {
            // 1. 生成规范化 UUID 文件名（不拼接用户原始文件名，防止特殊字符与路径注入）
            String originalFilename = file.getOriginalFilename();
            String suffix = FileUtil.getSuffix(originalFilename);
            String cleanExt = (suffix != null && !suffix.isEmpty()) ? suffix.toLowerCase() : "png";
            String newFilename = java.util.UUID.randomUUID().toString().replace("-", "") + "." + cleanExt;

            // 2. 构建相对路径（业务类型/用户ID/年月/文件名）
            String relativePath = buildRelativePath(bizType, userId, newFilename);

            // 3. 获取绝对路径（支持配置绝对路径）
            String uploadPath = properties.getLocal().getUploadPath();
            File baseDir = resolveBasePath(uploadPath);

            // 4. 创建完整路径
            File targetFile = new File(baseDir, relativePath);
            File parentDir = targetFile.getParentFile();

            // 5. 确保目录存在
            if (!parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                if (!created) {
                    log.error("创建目录失败: {}", parentDir.getAbsolutePath());
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建目录失败");
                }
                log.info("创建目录: {}", parentDir.getAbsolutePath());
            }

            // 6. 保存文件
            file.transferTo(targetFile);
            log.info("文件保存成功: {}", targetFile.getAbsolutePath());

            // 7. 返回访问URL
            String accessUrl = properties.getLocal().getUrlPrefix() + "/" +
                    relativePath.replace("\\", "/");
            return accessUrl;

        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public boolean deleteFile(String fileUrl) {
        try {
            if (fileUrl == null || fileUrl.trim().isEmpty()) {
                return false;
            }
            String cleanUrl = fileUrl.trim().replace("\\", "/");
            // 1. 提取相对路径（同时兼容完整前缀 URL 与数据库存储的相对 ObjectKey）
            String urlPrefix = properties.getLocal().getUrlPrefix();
            String relativePath = cleanUrl;
            if (urlPrefix != null && !urlPrefix.isEmpty()) {
                String normalizedPrefix = urlPrefix.endsWith("/") ? urlPrefix : urlPrefix + "/";
                if (cleanUrl.startsWith(normalizedPrefix)) {
                    relativePath = cleanUrl.substring(normalizedPrefix.length());
                } else if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
                    log.warn("文件URL不匹配本地存储前缀: {}", fileUrl);
                    return false;
                }
            }
            while (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
            if (relativePath.isEmpty() || relativePath.contains("..") || relativePath.indexOf('\0') >= 0) {
                log.warn("拒绝非法文件删除路径: {}", fileUrl);
                return false;
            }

            // 2. 获取文件绝对路径并严格校验规范化目录边界（防止路径遍历与兄弟目录逃逸）
            String uploadPath = properties.getLocal().getUploadPath();
            File baseDir = resolveBasePath(uploadPath);
            File targetFile = new File(baseDir, relativePath);

            String canonicalBasePath = baseDir.getCanonicalPath();
            String canonicalTargetPath = targetFile.getCanonicalPath();
            String baseBoundary = canonicalBasePath.endsWith(File.separator)
                    ? canonicalBasePath : canonicalBasePath + File.separator;
            if (!canonicalTargetPath.startsWith(baseBoundary)) {
                log.warn("检测到路径穿越删除尝试，已拦截: {}", fileUrl);
                return false;
            }

            // 3. 仅允许删除上传根目录内的普通文件
            if (targetFile.exists() && targetFile.isFile()) {
                boolean deleted = targetFile.delete();
                if (deleted) {
                    log.info("文件删除成功: {}", targetFile.getAbsolutePath());
                } else {
                    log.error("文件删除失败: {}", targetFile.getAbsolutePath());
                }
                return deleted;
            } else {
                log.warn("文件不存在或不是普通文件: {}", targetFile.getAbsolutePath());
                return false;
            }
        } catch (Exception e) {
            log.error("删除文件失败", e);
            return false;
        }
    }

    /**
     * 构建相对路径（业务类型/用户ID/年月/文件名）
     *
     * @param bizType  业务类型
     * @param userId   用户ID
     * @param filename 文件名
     * @return 相对路径
     */
    private String buildRelativePath(FileUploadBizEnum bizType, Long userId, String filename) {
        StringBuilder path = new StringBuilder();

        // 业务类型目录
        path.append(bizType.getValue()).append(File.separator);

        // 用户ID目录
        path.append(userId).append(File.separator);

        // 年月目录（如果启用）
        if (properties.getLocal().isEnableDatePath()) {
            String yearMonth = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyyMM")
            );
            path.append(yearMonth).append(File.separator);
        }

        // 文件名
        path.append(filename);

        return path.toString();
    }

    /**
     * 解析基础路径（支持绝对路径和相对路径）
     *
     * @param uploadPath 配置的上传路径
     * @return 解析后的绝对路径
     */
    private File resolveBasePath(String uploadPath) {
        File baseDir = new File(uploadPath);

        // 如果是绝对路径，直接使用
        if (baseDir.isAbsolute()) {
            log.info("使用绝对路径: {}", baseDir.getAbsolutePath());
            return baseDir;
        }

        // 如果是相对路径，相对于项目根目录
        String projectPath = System.getProperty("user.dir");
        baseDir = new File(projectPath, uploadPath);
        log.info("使用相对路径，解析为: {}", baseDir.getAbsolutePath());
        return baseDir;
    }
}
