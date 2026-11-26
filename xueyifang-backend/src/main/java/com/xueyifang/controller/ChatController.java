package com.xueyifang.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.domain.dto.chat.ChatSendRequest;
import com.xueyifang.domain.vo.ChatConversationVO;
import com.xueyifang.domain.vo.ChatMessageVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.service.IChatService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * <p>
 * 用户聊天记录表 前端控制器
 * </p>
 *
 * @author xueyifang
 * @since 2025-11-24
 */
@RestController
@RequestMapping("/chat")
@Api(tags = "聊天管理")
public class ChatController {

    @Resource
    private IChatService chatService;

    @PostMapping("/send")
    @ApiOperation("发送消息")
    public BaseResponse<Void> sendMessage(@RequestBody @Valid ChatSendRequest request) {
        chatService.sendMessage(request);
        return ResultUtils.success();
    }

    @GetMapping("/messages/{userId}")
    @ApiOperation("获取与指定用户的聊天记录")
    public BaseResponse<Page<ChatMessageVO>> getMessages(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "50") Integer pageSize) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        Page<ChatMessageVO> result = chatService.getChatMessagesWithUser(userId, pageNum, pageSize);
        return ResultUtils.success(result);
    }

    @GetMapping("/conversations")
    @ApiOperation("获取我的聊天会话列表")
    public BaseResponse<List<ChatConversationVO>> getConversations() {
        List<ChatConversationVO> result = chatService.getMyConversations();
        return ResultUtils.success(result);
    }
}
