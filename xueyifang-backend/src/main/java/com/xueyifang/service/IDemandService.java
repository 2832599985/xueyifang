package com.xueyifang.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xueyifang.domain.dto.DemandBidCreateRequest;
import com.xueyifang.domain.dto.DemandCreateRequest;
import com.xueyifang.domain.dto.DemandQueryRequest;
import com.xueyifang.domain.po.DemandRequest;
import com.xueyifang.domain.vo.DemandBidVO;
import com.xueyifang.domain.vo.DemandRequestVO;

import java.util.List;

public interface IDemandService extends IService<DemandRequest> {

    Long publishDemand(DemandCreateRequest request);

    Page<DemandRequestVO> pageDemands(DemandQueryRequest query);

    DemandRequestVO getDemandDetail(Long demandId);

    Long submitBid(Long demandId, DemandBidCreateRequest request);

    Long acceptBid(Long demandId, Long bidId);

    void cancelDemand(Long demandId, String reason);

    List<DemandRequestVO> listMyPublishedDemands();

    List<DemandBidVO> listMyBids();

    Page<DemandRequestVO> adminPageDemands(Integer pageNum, Integer pageSize, Integer status, String keyword);

    void adminCloseDemand(Long demandId, String reason);
}
