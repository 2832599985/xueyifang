package com.xueyifang.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.domain.dto.DisputeCreateRequest;
import com.xueyifang.domain.dto.DisputeHandleRequest;
import com.xueyifang.domain.dto.DisputeQueryRequest;
import com.xueyifang.domain.po.ServiceDispute;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xueyifang.domain.vo.ChatMessageVO;
import com.xueyifang.domain.vo.DisputeDetailVO;
import com.xueyifang.domain.vo.DisputeListItemVO;

import java.util.List;

/**
 * <p>
 * 服务纠纷表 服务类
 * </p>
 *
 * @author luoyi
 * @since 2025-11-19
 */
public interface IServiceDisputeService extends IService<ServiceDispute> {

    /**
     * 创建纠纷
     *
     * @param request 创建纠纷请求
     * @return 纠纷ID
     */
    Long createDispute(DisputeCreateRequest request);

    /**
     * 获取我的纠纷列表
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 纠纷列表
     */
    Page<DisputeListItemVO> getMyDisputes(Integer pageNum, Integer pageSize);

    /**
     * 获取纠纷详情
     *
     * @param disputeId 纠纷ID
     * @return 纠纷详情
     */
    DisputeDetailVO getDisputeDetail(Long disputeId);

    // ==================== 管理员方法 ====================

    /**
     * 管理员分页查询纠纷列表
     *
     * @param request 查询请求
     * @return 纠纷列表
     */
    Page<DisputeListItemVO> listDisputesForAdmin(DisputeQueryRequest request);

    /**
     * 管理员获取纠纷详情（无权限限制）
     *
     * @param disputeId 纠纷ID
     * @return 纠纷详情
     */
    DisputeDetailVO getDisputeDetailForAdmin(Long disputeId);

    /**
     * 根据订单ID获取纠纷详情（管理员）
     *
     * @param orderId 订单ID
     * @return 纠纷详情，无纠纷返回null
     */
    DisputeDetailVO getDisputeDetailByOrderId(Long orderId);

    /**
     * 管理员处理纠纷
     *
     * @param disputeId 纠纷ID
     * @param request 处理请求
     */
    void handleDisputeByAdmin(Long disputeId, DisputeHandleRequest request);

    /**
     * 管理员调阅纠纷关联订单买卖双方的站内沟通留痕记录
     *
     * @param disputeId 纠纷ID
     * @return 按时间升序排列的聊天快照
     */
    List<ChatMessageVO> getDisputeChatLogsForAdmin(Long disputeId);
}
