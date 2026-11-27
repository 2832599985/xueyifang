package com.xueyifang.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xueyifang.domain.dto.GroupSessionCreateRequest;
import com.xueyifang.domain.dto.GroupSessionQueryRequest;
import com.xueyifang.domain.po.GroupSession;
import com.xueyifang.domain.vo.GroupSessionVO;

import java.util.List;

public interface IGroupSessionService extends IService<GroupSession> {

    Long createGroupSession(GroupSessionCreateRequest request);

    Page<GroupSessionVO> pageGroupSessions(GroupSessionQueryRequest query);

    GroupSessionVO getGroupSessionDetail(Long sessionId);

    Long joinGroupSession(Long sessionId);

    void cancelGroupSession(Long sessionId, String reason);

    List<GroupSessionVO> listMyCreatedSessions();

    List<GroupSessionVO> listMyJoinedSessions();

    Page<GroupSessionVO> adminPageSessions(Integer pageNum, Integer pageSize, Integer status, String keyword);

    void adminCloseSession(Long sessionId, String reason);
}
