package com.xueyifang.controller;

import com.xueyifang.annotation.RateLimit;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.domain.dto.EduVerifyConfirmRequest;
import com.xueyifang.domain.dto.EduVerifySendRequest;
import com.xueyifang.domain.po.CreditScoreLog;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.service.ICreditScoreService;
import com.xueyifang.service.IUserService;
import com.xueyifang.utils.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Api(tags = "校园教育邮箱实名核验与信用分中心")
public class EduVerifyController {

    @Resource
    private IUserService userService;

    @Resource
    private ICreditScoreService creditScoreService;

    @PostMapping("/edu-verify/send")
    @RateLimit(maxRequests = 5, windowSeconds = 300, limitType = RateLimit.LimitType.USER)
    @ApiOperation("发送校园教育邮箱（.edu.cn）实名验证码")
    public BaseResponse<Map<String, Object>> sendEduCode(@RequestBody EduVerifySendRequest request) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        return ResultUtils.success(userService.sendEduVerifyCode(userId, request));
    }

    @PostMapping("/edu-verify/confirm")
    @RateLimit(maxRequests = 10, windowSeconds = 300, limitType = RateLimit.LimitType.USER)
    @ApiOperation("提交校园教育邮箱6位验证码完成学籍实名认证")
    public BaseResponse<Void> confirmEduCode(@RequestBody EduVerifyConfirmRequest request) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        userService.confirmEduVerify(userId, request);
        return ResultUtils.success();
    }

    @GetMapping("/credit-logs")
    @ApiOperation("查询我的校园履约信用分变动明细")
    public BaseResponse<List<CreditScoreLog>> getCreditLogs() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        return ResultUtils.success(creditScoreService.listMyCreditLogs(userId));
    }
}
