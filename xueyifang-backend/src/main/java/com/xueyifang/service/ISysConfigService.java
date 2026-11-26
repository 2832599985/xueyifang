package com.xueyifang.service;

import com.xueyifang.domain.po.SysConfig;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 系统配置表 服务类
 * </p>
 *
 * @author luoyi
 * @since 2025-11-21
 */
public interface ISysConfigService extends IService<SysConfig> {

    /**
     * 获取配置值(字符串)
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    String getConfigValue(String key, String defaultValue);

    /**
     * 获取配置值(整数)
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    int getIntValue(String key, int defaultValue);

    /**
     * 删除配置缓存
     *
     * @param key 配置键
     */
    void deleteConfigCache(String key);
}
