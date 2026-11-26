package com.xueyifang.mapper;

import com.xueyifang.domain.po.ServiceDispute;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 服务纠纷表 Mapper 接口
 * </p>
 *
 * @author luoyi
 * @since 2025-11-19
 */
public interface ServiceDisputeMapper extends BaseMapper<ServiceDispute> {

    /**
     * 悲观锁查询纠纷记录（SELECT ... FOR UPDATE）
     */
    @Select("SELECT * FROM service_dispute WHERE id = #{disputeId} AND is_delete = 0 FOR UPDATE")
    ServiceDispute selectForUpdate(@Param("disputeId") Long disputeId);
}
