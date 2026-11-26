package com.xueyifang.mapper;

import com.xueyifang.domain.po.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 用户表 Mapper 接口
 * </p>
 *
 * @author luoyi
 * @since 2025-10-27
 */
public interface UserMapper extends BaseMapper<User> {

    /**
     * 使用悲观锁查询用户（FOR UPDATE）
     * 用于钱包余额更新等需要并发控制的场景
     *
     * @param userId 用户ID
     * @return 用户对象
     */
    User selectForUpdate(@Param("userId") Long userId);

}
