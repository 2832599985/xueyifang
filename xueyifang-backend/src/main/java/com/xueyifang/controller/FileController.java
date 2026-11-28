package com.xueyifang.controller;

import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.config.FileStorageProperties;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.service.FileService;
import com.xueyifang.service.IUserService;
import com.xueyifang.utils.UserContext;
import com.xueyifang.domain.dto.UserUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * 文件管理控制器
 *
 * @author xueyifang
 */
@RestController
@RequestMapping("/file")
@Tag(name = "文件管理")
@Slf4j
public class FileController {

    @Resource
    private FileService fileService;

    @Resource
    private IUserService userService;

    @Resource
    private FileStorageProperties properties;

    @Resource
    private com.xueyifang.utils.FileUrlResolver fileUrlResolver;

    /**
     * 上传单个文件
     */
    @PostMapping("/upload")
    @Operation(summary = "上传单个文件")
    public BaseResponse<String> uploadFile(
            @RequestPart("file") @Parameter(description = "文件") MultipartFile file,
            @RequestParam("biz") @Parameter(description = "业务类型(user_avatar/service_image)") String biz
    ) {
        // 获取当前登录用户ID
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }

        // 上传文件
        String fileUrl = fileService.uploadFile(file, biz, userId);
        if ("user_avatar".equals(biz)) {
            // 获取旧头像路径（用于后续清理）
            String oldAvatar = userService.getById(userId).getAvatar();

            // 更新用户头像为新路径（数据库仅存相对 ObjectKey）
            UserUpdateRequest updateRequest = new UserUpdateRequest();
            String relative = fileUrlResolver.toRelativeKey(fileUrl);
            updateRequest.setAvatar(relative);
            userService.updateUserProfile(userId, updateRequest);

            // 异步删除旧头像文件（若存在且不同于新文件）
            if (oldAvatar != null && !oldAvatar.isEmpty() && !oldAvatar.equals(relative)) {
                try {
                    fileService.deleteFile(oldAvatar);
                    log.info("已清理旧头像文件: {}", oldAvatar);
                } catch (Exception e) {
                    // 旧文件删除失败不影响主流程
                    log.warn("清理旧头像文件失败: {}", oldAvatar, e);
                }
            }
        }
        return new BaseResponse<>(ErrorCode.SUCCESS.getCode(), "success", fileUrl);
    }

    /**
     * 批量上传文件
     */
    @PostMapping("/upload/batch")
    @Operation(summary = "批量上传文件")
    public BaseResponse<List<String>> uploadFiles(
            @RequestPart("files") @Parameter(description = "文件列表") List<MultipartFile> files,
            @RequestParam("biz") @Parameter(description = "业务类型(user_avatar/service_image)") String biz
    ) {
        // 获取当前登录用户ID
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }

        // 批量上传文件
        List<String> fileUrls = fileService.uploadFiles(files, biz, userId);
        return ResultUtils.success(fileUrls);
    }

    /**
     * 删除文件（严格校验资源归属权与相对路径合法性，禁止跨用户删文件与路径穿越）
     */
    @DeleteMapping("/delete")
    @Operation(summary = "删除文件")
    public BaseResponse<Boolean> deleteFile(
            @RequestParam("url") @Parameter(description = "文件URL") String url
    ) {
        // 获取当前登录用户ID
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        if (url == null || url.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件URL不能为空");
        }

        String relativeKey = fileUrlResolver.toRelativeKey(url);
        if (relativeKey == null || relativeKey.isEmpty()
                || relativeKey.contains("..")
                || relativeKey.contains("\\")
                || relativeKey.contains("://")
                || relativeKey.indexOf('\0') >= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法的文件路径");
        }

        // 文件存储结构固定为 {biz}/{userId}/{yyyyMM}/{uuid}.{ext}，仅允许本人或管理员删除
        if (!UserContext.isAdmin()) {
            String[] segments = relativeKey.split("/");
            if (segments.length < 3 || !String.valueOf(userId).equals(segments[1])) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权删除他人上传的文件");
            }
        }

        // 删除文件（统一传递安全校验后的相对路径或受控公网 URL）
        boolean result = fileService.deleteFile(fileUrlResolver.toPublicUrl(relativeKey));
        return ResultUtils.success(result);
    }

    /**
     * 查看本地文件（仅本地存储模式）
     * 访问路径示例：/api/file/view/user_avatar/1/202511/abc123-avatar.jpg
     */
    @GetMapping("/view/**")
    @Operation(summary = "查看文件", description = "对象存储模式下自动302重定向至远程对象存储，本地模式下直接流式输出")
    public void viewFile(HttpServletRequest request, HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        String prefix = contextPath + "/file/view/";
        String relativePath = requestURI.startsWith(prefix) ? requestURI.substring(prefix.length()) : "";

        if (relativePath.isEmpty()
                || relativePath.contains("..")
                || relativePath.contains("\\")
                || relativePath.contains("://")
                || relativePath.startsWith("/")
                || relativePath.contains("\r")
                || relativePath.contains("\n")
                || relativePath.indexOf('\0') >= 0) {
            log.warn("非法文件预览路径: {}", relativePath);
            try {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "非法的文件路径");
            } catch (IOException e) {
                log.error("发送错误响应失败", e);
            }
            return;
        }

        // 若启用了远程 S3/OSS 对象存储，仅允许重定向到配置的对象存储前缀内（严防开放重定向）
        if (!"local".equalsIgnoreCase(properties.getType())) {
            try {
                String remoteUrl = fileUrlResolver.toPublicUrl(relativePath);
                if (remoteUrl == null || !isTrustedRemoteStorageUrl(remoteUrl)) {
                    log.warn("拦截非受信任的远程对象存储重定向: {}", remoteUrl);
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "非法的对象存储地址");
                    return;
                }
                response.setHeader("Cache-Control", "public, max-age=31536000, immutable");
                response.sendRedirect(remoteUrl);
            } catch (IOException e) {
                log.error("重定向至远程对象存储失败", e);
            }
            return;
        }

        try {
            // 2. 解析本地文件路径
            String uploadPath = properties.getLocal().getUploadPath();
            File baseDir = resolveBasePath(uploadPath);
            File targetFile = new File(baseDir, relativePath);

            // 3. 安全检查：严格目录边界校验，防止路径遍历与同前缀兄弟目录逃逸
            String canonicalBasePath = baseDir.getCanonicalPath();
            String canonicalFilePath = targetFile.getCanonicalPath();
            String baseBoundary = canonicalBasePath.endsWith(File.separator)
                    ? canonicalBasePath : canonicalBasePath + File.separator;
            if (!canonicalFilePath.startsWith(baseBoundary)) {
                log.warn("非法文件访问尝试: {}", relativePath);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "无权访问该文件");
                return;
            }

            // 4. 检查文件是否存在
            if (!targetFile.exists() || !targetFile.isFile()) {
                log.warn("文件不存在: {}", targetFile.getAbsolutePath());
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "文件不存在");
                return;
            }

            // 5. 设置响应头
            String filename = targetFile.getName();
            String contentType = getContentType(filename);
            response.setContentType(contentType);
            response.setContentLengthLong(targetFile.length());
            // 设置为内联显示（浏览器中直接显示图片，而不是下载）
            response.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");

            // 6. 输出文件内容
            try (FileInputStream fis = new FileInputStream(targetFile);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }

            log.debug("文件访问成功: {}", targetFile.getAbsolutePath());

        } catch (IOException e) {
            log.error("读取文件失败", e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "读取文件失败");
            } catch (IOException ex) {
                log.error("发送错误响应失败", ex);
            }
        }
    }

    /**
     * 校验远程对象存储重定向目标必须位于已配置的 OSS/CDN 前缀下，防止开放重定向漏洞
     */
    private boolean isTrustedRemoteStorageUrl(String remoteUrl) {
        if (remoteUrl == null || remoteUrl.contains("\r") || remoteUrl.contains("\n")) {
            return false;
        }
        FileStorageProperties.OssStorageConfig oss = properties.getOss();
        if (oss == null) {
            return false;
        }
        String urlPrefix = oss.getUrlPrefix();
        if (urlPrefix != null && !urlPrefix.trim().isEmpty()) {
            String normalized = urlPrefix.trim().endsWith("/") ? urlPrefix.trim() : urlPrefix.trim() + "/";
            if (remoteUrl.startsWith(normalized)) {
                return true;
            }
        }
        String endpoint = oss.getEndpoint();
        String bucket = oss.getBucketName();
        if (endpoint != null && !endpoint.trim().isEmpty() && bucket != null && !bucket.trim().isEmpty()) {
            String ep = endpoint.trim().replaceAll("/+$", "");
            if (!ep.startsWith("http://") && !ep.startsWith("https://")) {
                ep = "https://" + ep;
            }
            String pathStyle = ep + "/" + bucket.trim() + "/";
            if (remoteUrl.startsWith(pathStyle)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析基础路径（支持绝对路径和相对路径）
     */
    private File resolveBasePath(String uploadPath) {
        File baseDir = new File(uploadPath);
        if (baseDir.isAbsolute()) {
            return baseDir;
        }
        String projectPath = System.getProperty("user.dir");
        return new File(projectPath, uploadPath);
    }

    /**
     * 根据文件名获取Content-Type
     */
    private String getContentType(String filename) {
        String lowerFilename = filename.toLowerCase();
        if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerFilename.endsWith(".png")) {
            return "image/png";
        } else if (lowerFilename.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerFilename.endsWith(".webp")) {
            return "image/webp";
        } else if (lowerFilename.endsWith(".svg")) {
            return "image/svg+xml";
        } else {
            return "application/octet-stream";
        }
    }

    /**
     * 将完整访问URL转换为相对存储路径，便于数据库仅存相对路径
     */
    private String toRelativePath(String fileUrl) {
        try {
            String type = properties.getType();
            if ("local".equalsIgnoreCase(type) && properties.getLocal() != null) {
                String prefix = properties.getLocal().getUrlPrefix();
                if (prefix != null && !prefix.isEmpty()) {
                    String p = prefix.endsWith("/") ? prefix : prefix + "/";
                    if (fileUrl.startsWith(p)) {
                        return fileUrl.substring(p.length());
                    }
                }
            } else if ("oss".equalsIgnoreCase(type) && properties.getOss() != null) {
                String urlPrefix = properties.getOss().getUrlPrefix();
                if (urlPrefix != null && !urlPrefix.isEmpty()) {
                    String p = urlPrefix.endsWith("/") ? urlPrefix : urlPrefix + "/";
                    if (fileUrl.startsWith(p)) {
                        return fileUrl.substring(p.length());
                    }
                } else {
                    String bucket = properties.getOss().getBucketName();
                    String endpoint = properties.getOss().getEndpoint();
                    if (bucket != null && endpoint != null) {
                        String defaultPrefix = "https://" + bucket + "." + endpoint + "/";
                        if (fileUrl.startsWith(defaultPrefix)) {
                            return fileUrl.substring(defaultPrefix.length());
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return fileUrl;
    }
}
