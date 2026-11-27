package com.xueyifang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xueyifang.domain.po.GroupSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GroupSessionMapper extends BaseMapper<GroupSession> {

    @Select("SELECT * FROM group_session WHERE id = #{id} AND is_delete = 0 FOR UPDATE")
    GroupSession selectForUpdate(@Param("id") Long id);
}
