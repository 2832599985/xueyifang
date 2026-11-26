package com.xueyifang.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.annotation.AuthCheck;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.constant.UserConstant;
import com.xueyifang.domain.dto.DisputeHandleRequest;
import com.xueyifang.domain.dto.DisputeQueryRequest;
import com.xueyifang.domain.vo.ChatMessageVO;
import com.xueyifang.domain.vo.DisputeDetailVO;
import com.xueyifang.domain.vo.DisputeListItemVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.service.IServiceDisputeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * 管理员纠纷处理控制器
 * <p>
 * 提供管理员视角的纠纷管理功能：
 * <ul>
 *   <li>分页查询所有纠纷</li>
 *   <li>查看纠纷详情</li>
 *   <li>处理纠纷（解决/驳回，可选退款）</li>
 * </ul>
 *
 * @author xueyifang
 */
@RestController
@RequestMapping("/admin/dispute")
@Api(tags = "管理员纠纷处理")
public class AdminDisputeController {

    @Resource
    private IServiceDisputeService serviceDisputeService;

    /**
     * 分页查询纠纷列表（管理员）
     * <p>
     * 支持按纠纷状态、订单编号、发起人ID、纠纷类型筛选
     *
     * @param request 查询请求
     * @return 纠纷列表
     */
    @GetMapping("/list")
    @ApiOperation("分页查询纠纷列表")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    public BaseResponse<Page<DisputeListItemVO>> listDisputes(DisputeQueryRequest request) {
        if (request == null) {
            request = new DisputeQueryRequest();
        }
        // 参数校验
        if (request.getPageNum() < 1) {
            request.setPageNum(1);
        }
        if (request.getPageSize() < 1 || request.getPageSize() > 100) {
            request.setPageSize(10);
        }
        Page<DisputeListItemVO> result = serviceDisputeService.listDisputesForAdmin(request);
        return ResultUtils.success(result);
    }

    /**
     * 获取纠纷详情（管理员）
     * <p>
     * 管理员可以查看任意纠纷的详情，不受买家/卖家权限限制
     *
     * @param disputeId 纠纷ID
     * @return 纠纷详情
     */
    @GetMapping("/{disputeId}")
    @ApiOperation("获取纠纷详情")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    public BaseResponse<DisputeDetailVO> getDisputeDetail(
            @ApiParam("纠纷ID") @PathVariable Long disputeId) {
        if (disputeId == null || disputeId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "纠纷ID无效");
        }
        DisputeDetailVO result = serviceDisputeService.getDisputeDetailForAdmin(disputeId);
        return ResultUtils.success(result);
    }

    /**
     * 根据订单ID获取纠纷详情（管理员）
     * <p>
     * 用于在管理员订单详情页面查看关联的纠纷
     *
     * @param orderId 订单ID
     * @return 纠纷详情，如果无纠纷返回null
     */
    @GetMapping("/by-order/{orderId}")
    @ApiOperation("根据订单ID获取纠纷详情")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    public BaseResponse<DisputeDetailVO> getDisputeByOrderId(
            @ApiParam("订单ID") @PathVariable Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单ID无效");
        }
        DisputeDetailVO result = serviceDisputeService.getDisputeDetailByOrderId(orderId);
        return ResultUtils.success(result);
    }

    /**
     * 处理纠纷（管理员）
     * <p>
     * 对纠纷执行「处理」动作：
     * <ul>
     *   <li>标记纠纷为已解决（actionType=1）或已拒绝（actionType=2）</li>
     *   <li>填写管理员回复（adminReply）与解决方案（resolution）</li>
     *   <li>必要时对订单执行退款（needRefund=true，仅线上担保订单）</li>
     *   <li>处理完成后，向买家和卖家发送通知</li>
     * </ul>
     *
     * @param disputeId 纠纷ID
     * @param request   处理请求
     * @return 处理结果
     */
    @PostMapping("/{disputeId}/handle")
    @ApiOperation("处理纠纷")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    public BaseResponse<Void> handleDispute(
            @ApiParam("纠纷ID") @PathVariable Long disputeId,
            @Valid @RequestBody DisputeHandleRequest request) {
        if (disputeId == null || disputeId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "纠纷ID无效");
        }
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        serviceDisputeService.handleDisputeByAdmin(disputeId, request);
        return ResultUtils.success(null);
    }

    /**
     * 调阅纠纷关联订单买卖双方的站内沟通留痕记录（管理员仲裁取证专用）
     */
    @GetMapping("/{disputeId}/chat-logs")
    @ApiOperation("调阅纠纷关联订单的买卖双方沟通留痕记录")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    public BaseResponse<List<ChatMessageVO>> getDisputeChatLogs(
            @ApiParam("纠纷ID") @PathVariable Long disputeId) {
        if (disputeId == null || disputeId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "纠纷ID无效");
        }
        return ResultUtils.success(serviceDisputeService.getDisputeChatLogsForAdmin(disputeId));
    }
}
