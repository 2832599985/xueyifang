package com.xueyifang.mapper;

import com.xueyifang.domain.po.Service;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 服务表 Mapper 接口
 * </p>
 *
 * @author luoyi
 * @since 2025-11-17
 */
public interface ServiceMapper extends BaseMapper<Service> {

    /**
     * 原子性增加服务购买次数（带库存校验）
     * <p>
     * 在同一条 UPDATE 中判断剩余库存，依赖行锁保证并发下不会超卖。
     * max_purchases = -1 表示不限量。
     *
     * @param serviceId 服务ID
     * @param quantity  增加数量
     * @return 影响行数，0 表示库存不足或服务不存在
     */
    @Update("UPDATE service SET current_purchase_count = current_purchase_count + #{quantity} " +
            "WHERE id = #{serviceId} AND is_delete = 0 " +
            "AND (max_purchases = -1 OR current_purchase_count + #{quantity} <= max_purchases)")
    int incrementPurchaseCountIfAvailable(@Param("serviceId") Long serviceId, @Param("quantity") Integer quantity);

    /**
     * 原子性回退服务购买次数（退款时归还库存），不会减为负数
     *
     * @param serviceId 服务ID
     * @param quantity  回退数量
     * @return 影响行数
     */
    @Update("UPDATE service SET current_purchase_count = GREATEST(current_purchase_count - #{quantity}, 0) " +
            "WHERE id = #{serviceId}")
    int decrementPurchaseCount(@Param("serviceId") Long serviceId, @Param("quantity") Integer quantity);

    /**
     * 原子性更新服务平均评分
     * 使用 SQL 子查询聚合防止并发问题
     *
     * @param serviceId 服务ID
     * @return 影响行数
     */
    @Update("UPDATE service SET average_rating = (SELECT COALESCE(AVG(rating), 0) FROM service_review WHERE service_id = #{serviceId} AND is_delete = 0) WHERE id = #{serviceId}")
    int updateAverageRating(@Param("serviceId") Long serviceId);

    /**
     * 按天统计新增服务数
     *
     * @param start 起始时间（包含）
     * @return 每行包含 stat_date(yyyy-MM-dd)、service_count
     */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS stat_date, COUNT(*) AS service_count " +
            "FROM service WHERE is_delete = 0 AND create_time >= #{start} " +
            "GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d')")
    List<Map<String, Object>> countDailyCreated(@Param("start") LocalDateTime start);

}
