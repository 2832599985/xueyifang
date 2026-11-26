package com.xueyifang.service;

import com.xueyifang.domain.vo.admin.UserImportResultVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户批量导入服务接口
 *
 * @author xueyifang
 */
public interface IUserImportService {

    /**
     * 批量导入用户
     *
     * @param file Excel(.xlsx) 或 CSV(UTF-8) 文件
     * @return 导入结果
     */
    UserImportResultVO importUsers(MultipartFile file);
}
