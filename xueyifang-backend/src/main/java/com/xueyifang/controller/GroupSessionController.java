package com.xueyifang.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.domain.dto.GroupSessionCreateRequest;
import com.xueyifang.domain.dto.GroupSessionQueryRequest;
import com.xueyifang.domain.vo.GroupSessionVO;
import com.xueyifang.service.IGroupSessionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/group-session")
@Api(tags = "校内多人拼班团课与共享辅导档期接口")
public class GroupSessionController {

    @Resource
    private IGroupSessionService groupSessionService;

    @GetMapping("/list")
    @ApiOperation("分页查询校内多人拼班团课大厅")
    public BaseResponse<Page<GroupSessionVO>> listSessions(GroupSessionQueryRequest query) {
        return ResultUtils.success(groupSessionService.pageGroupSessions(query));
    }

    @GetMapping("/detail/{id}")
    @ApiOperation("查询拼班团课详情与已参团学员列表")
    public BaseResponse<GroupSessionVO> getDetail(@PathVariable("id") Long id) {
        return ResultUtils.success(groupSessionService.getGroupSessionDetail(id));
    }

    @PostMapping("/create")
    @ApiOperation("认证创作者发起校内多人拼班团课（含阶梯优惠价与真实地图围栏）")
    public BaseResponse<Long> createSession(@RequestBody GroupSessionCreateRequest request) {
        return ResultUtils.success(groupSessionService.createGroupSession(request));
    }

    @PostMapping("/{id}/join")
    @ApiOperation("学生报名参团并自动生成担保订单（满阶梯人数自动全员退差价）")
    public BaseResponse<Long> joinSession(@PathVariable("id") Long id) {
        return ResultUtils.success(groupSessionService.joinGroupSession(id));
    }

    @PostMapping("/{id}/cancel")
    @ApiOperation("创作者取消尚未有人报名的拼班团课")
    public BaseResponse<Boolean> cancelSession(
            @PathVariable("id") Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        groupSessionService.cancelGroupSession(id, reason);
        return ResultUtils.success(true);
    }

    @GetMapping("/my-created")
    @ApiOperation("查询我开办的拼班团课")
    public BaseResponse<List<GroupSessionVO>> listMyCreated() {
        return ResultUtils.success(groupSessionService.listMyCreatedSessions());
    }

    @GetMapping("/my-joined")
    @ApiOperation("查询我报名参团的拼班团课")
    public BaseResponse<List<GroupSessionVO>> listMyJoined() {
        return ResultUtils.success(groupSessionService.listMyJoinedSessions());
    }
}
