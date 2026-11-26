package com.xueyifang.mapper;

import com.xueyifang.domain.po.ServiceOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 服务订单表 Mapper 接口
 * </p>
 *
 * @author luoyi
 * @since 2025-11-19
 */
public interface ServiceOrderMapper extends BaseMapper<ServiceOrder> {

    /**
     * 批量统计服务销量（GROUP BY 优化，避免 N+1 查询）
     * @param serviceIds 服务ID列表
     * @param orderStatus 订单状态（已完成）
     * @return List<Map>，每个 Map 包含 service_id 和 sales_count
     */
    List<Map<String, Object>> batchCountSalesByServiceIds(
            @Param("serviceIds") List<Long> serviceIds,
            @Param("orderStatus") Integer orderStatus
    );

    /**
     * 悲观锁查询订单（SELECT ... FOR UPDATE）
     * <p>
     * 所有会改变订单状态/资金的操作都必须先锁订单行，再锁用户行，
     * 保证"先检查状态、再修改"的过程不会被并发请求穿插（防止重复支付、重复退款等）。
     *
     * @param orderId 订单ID
     * @return 订单，不存在返回 null
     */
    @Select("SELECT * FROM service_order WHERE id = #{orderId} AND is_delete = 0 FOR UPDATE")
    ServiceOrder selectForUpdate(@Param("orderId") Long orderId);

    /**
     * 统计指定状态的订单数量与金额总和
     *
     * @param orderStatus 订单状态
     * @param sellerId    卖家ID，为 null 时统计全平台
     * @return 包含 order_count、total_amount 两个键
     */
    @Select("<script>" +
            "SELECT COUNT(*) AS order_count, COALESCE(SUM(total_amount), 0) AS total_amount " +
            "FROM service_order WHERE is_delete = 0 AND order_status = #{orderStatus} " +
            "<if test='sellerId != null'>AND seller_id = #{sellerId}</if>" +
            "</script>")
    Map<String, Object> countAndSumByStatus(@Param("orderStatus") Integer orderStatus,
                                            @Param("sellerId") Long sellerId);

    /**
     * 统计指定时间之后有过订单（买家或卖家）的去重用户数
     *
     * @param since 起始时间
     * @return 活跃用户数
     */
    @Select("SELECT COUNT(DISTINCT uid) FROM (" +
            "SELECT buyer_id AS uid FROM service_order WHERE is_delete = 0 AND create_time >= #{since} " +
            "UNION ALL " +
            "SELECT seller_id AS uid FROM service_order WHERE is_delete = 0 AND create_time >= #{since}" +
            ") t")
    Long countActiveUsersSince(@Param("since") LocalDateTime since);

    /**
     * 按天统计订单数与已完成订单金额
     *
     * @param start           起始时间（包含）
     * @param completedStatus 已完成状态值
     * @return 每行包含 stat_date(yyyy-MM-dd)、order_count、completed_amount
     */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS stat_date, COUNT(*) AS order_count, " +
            "COALESCE(SUM(CASE WHEN order_status = #{completedStatus} THEN total_amount ELSE 0 END), 0) AS completed_amount " +
            "FROM service_order WHERE is_delete = 0 AND create_time >= #{start} " +
            "GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d')")
    List<Map<String, Object>> dailyOrderStats(@Param("start") LocalDateTime start,
                                              @Param("completedStatus") Integer completedStatus);
}
