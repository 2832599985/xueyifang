package com.xueyifang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xueyifang.domain.po.DemandRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DemandRequestMapper extends BaseMapper<DemandRequest> {

    @Select("SELECT * FROM demand_request WHERE id = #{id} AND is_delete = 0 FOR UPDATE")
    DemandRequest selectForUpdate(@Param("id") Long id);
}
