package com.xueyifang.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xueyifang.domain.dto.MeetupProposalCreateRequest;
import com.xueyifang.domain.dto.MeetupProposalRespondRequest;
import com.xueyifang.domain.po.OrderMeetupProposal;
import com.xueyifang.domain.vo.MeetupProposalVO;

import java.util.List;

public interface IOrderMeetupProposalService extends IService<OrderMeetupProposal> {

    Long proposeMeetupLocation(Long orderId, MeetupProposalCreateRequest request);

    void respondMeetupProposal(Long proposalId, MeetupProposalRespondRequest request);

    List<MeetupProposalVO> listOrderProposals(Long orderId);
}
