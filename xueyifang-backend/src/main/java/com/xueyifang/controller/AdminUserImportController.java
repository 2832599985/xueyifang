package com.xueyifang.controller;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.xueyifang.annotation.AuthCheck;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.constant.UserConstant;
import com.xueyifang.domain.vo.admin.UserImportResultVO;
import com.xueyifang.service.IUserImportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 管理员-用户批量导入控制器
 *
 * @author xueyifang
 */
@Slf4j
@RestController
@RequestMapping("/admin/user-import")
@Api(tags = "管理员-用户批量导入")
public class AdminUserImportController {

    @Resource
    private IUserImportService userImportService;

    /**
     * 批量导入用户
     *
     * @param file Excel(.xlsx) 或 CSV(UTF-8) 文件
     * @return 导入结果
     */
    @PostMapping("/upload")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("批量导入用户")
    public BaseResponse<UserImportResultVO> importUsers(@RequestParam("file") MultipartFile file) {
        log.info("管理员批量导入用户，文件名: {}", file.getOriginalFilename());
        UserImportResultVO result = userImportService.importUsers(file);
        return ResultUtils.success(result);
    }

    /**
     * 下载导入模板
     *
     * @param format 模板格式：xlsx（默认）或 csv
     * @return 模板文件
     */
    @GetMapping("/template")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("下载导入模板")
    public ResponseEntity<byte[]> downloadTemplate(
            @ApiParam(value = "模板格式: xlsx 或 csv", defaultValue = "xlsx")
            @RequestParam(value = "format", defaultValue = "xlsx") String format) {

        log.info("管理员下载用户导入模板，格式: {}", format);

        if ("csv".equalsIgnoreCase(format)) {
            return downloadCsvTemplate();
        } else {
            return downloadExcelTemplate();
        }
    }

    /**
     * 生成 CSV 模板
     */
    private ResponseEntity<byte[]> downloadCsvTemplate() {
        StringBuilder sb = new StringBuilder();
        // 表头
        sb.append("studentId,realName,phone,professionalId,password,email,dormitory,grade,nickname\n");
        // 示例数据
        sb.append("20240001,张三,13800138001,1,Test1234,zhangsan@example.com,宿舍1-101,大一,\n");
        sb.append("20240002,李四,13800138002,2,Test1234,lisi@example.com,宿舍2-202,大二,小李\n");

        byte[] content = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "user_import_template.csv");
        headers.setContentLength(content.length);

        return ResponseEntity.ok().headers(headers).body(content);
    }

    /**
     * 生成 Excel 模板
     */
    private ResponseEntity<byte[]> downloadExcelTemplate() {
        // 表头
        List<String> headerRow = Arrays.asList(
                "studentId", "realName", "phone", "professionalId", "password",
                "email", "dormitory", "grade", "nickname"
        );

        // 中文说明行
        List<String> descRow = Arrays.asList(
                "学号(必填)", "姓名(必填)", "手机号(必填)", "专业ID(必填)", "密码(必填)",
                "邮箱(可选)", "寝室号(可选)", "年级(可选)", "昵称(可选)"
        );

        // 示例数据行
        List<String> exampleRow1 = Arrays.asList(
                "20240001", "张三", "13800138001", "1", "Test1234",
                "zhangsan@example.com", "宿舍1-101", "大一", ""
        );
        List<String> exampleRow2 = Arrays.asList(
                "20240002", "李四", "13800138002", "2", "Test1234",
                "lisi@example.com", "宿舍2-202", "大二", "小李"
        );

        // 使用 Hutool 创建 Excel
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ExcelWriter writer = ExcelUtil.getWriter(true); // true = xlsx 格式

        // 写入数据
        writer.writeRow(headerRow);
        writer.writeRow(descRow);
        writer.writeRow(exampleRow1);
        writer.writeRow(exampleRow2);

        // 设置列宽
        for (int i = 0; i < headerRow.size(); i++) {
            writer.setColumnWidth(i, 18);
        }

        writer.flush(outputStream);
        writer.close();

        byte[] content = outputStream.toByteArray();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "user_import_template.xlsx");
        headers.setContentLength(content.length);

        return ResponseEntity.ok().headers(headers).body(content);
    }
}
