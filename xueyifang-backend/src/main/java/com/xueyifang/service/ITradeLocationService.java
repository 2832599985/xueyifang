package com.xueyifang.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.domain.dto.TradeLocationAddRequest;
import com.xueyifang.domain.dto.TradeLocationUpdateRequest;
import com.xueyifang.domain.po.TradeLocation;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xueyifang.domain.vo.TradeLocationVO;

import java.util.List;

/**
 * <p>
 * 交易地点表 服务类
 * </p>
 *
 * @author luoyi
 * @since 2025-10-29
 */
public interface ITradeLocationService extends IService<TradeLocation> {

    /**
     * 查询所有可用的交易地点（未删除的）
     *
     * @return 交易地点VO列表
     */
    List<TradeLocationVO> listAllTradeLocations();

    /**
     * 查询所有交易地点（包括不可用的，但不包括已删除的）
     *
     * @return 交易地点VO列表
     */
    List<TradeLocationVO> listAllTradeLocationsIncludingUnavailable();

    /**
     * 分页查询交易地点
     *
     * @param current 当前页码
     * @param pageSize 每页大小
     * @param isAvailable 是否可用（null表示查询全部）
     * @return 分页结果
     */
    Page<TradeLocationVO> pageTradeLocations(Long current, Long pageSize, Integer isAvailable);

    /**
     * 管理端分页查询交易地点
     *
     * @param current 当前页码
     * @param pageSize 每页大小
     * @param isAvailable 是否可用（null表示查询全部）
     * @param nameLike 地点名称模糊匹配
     * @return 分页结果
     */
    Page<TradeLocation> pageTradeLocationsForAdmin(Long current, Long pageSize, Integer isAvailable, String nameLike);

    /**
     * 根据ID查询交易地点详情
     *
     * @param id 地点ID
     * @return 交易地点信息
     */
    TradeLocation getTradeLocationById(Long id);

    /**
     * 添加交易地点
     *
     * @param addRequest 添加请求
     * @return 是否成功
     */
    boolean addTradeLocation(TradeLocationAddRequest addRequest);

    /**
     * 更新交易地点信息
     *
     * @param updateRequest 更新请求
     * @return 是否成功
     */
    boolean updateTradeLocation(TradeLocationUpdateRequest updateRequest);

    /**
     * 删除交易地点（逻辑删除）
     *
     * @param id 地点ID
     * @return 是否成功
     */
    boolean deleteTradeLocation(Long id);

    /**
     * 批量删除交易地点（逻辑删除）
     *
     * @param ids 地点ID列表
     * @return 是否成功
     */
    boolean batchDeleteTradeLocations(List<Long> ids);
}
