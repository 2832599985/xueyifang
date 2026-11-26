package com.xueyifang.domain.vo.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量导入用户结果返回对象
 *
 * @author xueyifang
 */
@Data
@ApiModel(value = "用户导入结果", description = "批量导入用户返回的汇总和错误信息")
public class UserImportResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 总数
     */
    @ApiModelProperty(value = "总数", example = "100")
    private Integer totalCount = 0;

    /**
     * 成功数
     */
    @ApiModelProperty(value = "成功数", example = "95")
    private Integer successCount = 0;

    /**
     * 跳过数（学号已存在）
     */
    @ApiModelProperty(value = "跳过数（学号已存在）", example = "3")
    private Integer skippedCount = 0;

    /**
     * 失败数（校验失败）
     */
    @ApiModelProperty(value = "失败数", example = "2")
    private Integer failedCount = 0;

    /**
     * 失败明细列表
     */
    @ApiModelProperty(value = "失败明细")
    private List<FailedRow> failedRows = new ArrayList<>();

    /**
     * 失败行详情
     */
    @Data
    @ApiModel(value = "失败行详情")
    public static class FailedRow implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 行号
         */
        @ApiModelProperty(value = "行号", example = "5")
        private Integer rowNum;

        /**
         * 学号（如果有）
         */
        @ApiModelProperty(value = "学号")
        private String studentId;

        /**
         * 失败原因
         */
        @ApiModelProperty(value = "失败原因", example = "密码格式不正确")
        private String reason;

        public FailedRow() {
        }

        public FailedRow(Integer rowNum, String studentId, String reason) {
            this.rowNum = rowNum;
            this.studentId = studentId;
            this.reason = reason;
        }
    }

    /**
     * 添加成功计数
     */
    public void addSuccess() {
        this.successCount++;
        this.totalCount++;
    }

    /**
     * 添加跳过计数
     */
    public void addSkipped() {
        this.skippedCount++;
        this.totalCount++;
    }

    /**
     * 添加失败记录
     */
    public void addFailed(Integer rowNum, String studentId, String reason) {
        this.failedCount++;
        this.totalCount++;
        this.failedRows.add(new FailedRow(rowNum, studentId, reason));
    }
}
