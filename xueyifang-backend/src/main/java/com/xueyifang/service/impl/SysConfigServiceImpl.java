package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xueyifang.constant.CacheConstant;
import com.xueyifang.domain.po.SysConfig;
import com.xueyifang.mapper.SysConfigMapper;
import com.xueyifang.service.ISysConfigService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xueyifang.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * <p>
 * 系统配置表 服务实现类
 * </p>
 *
 * @author luoyi
 * @since 2025-11-21
 */
@Service
@Slf4j
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements ISysConfigService {

    @Resource
    private RedisUtils redisUtils;

    @Override
    public String getConfigValue(String key, String defaultValue) {
        // 1. 尝试从缓存获取
        String cacheKey = CacheConstant.CONFIG_PREFIX + key;
        Object cachedValue = redisUtils.get(cacheKey);
        if (cachedValue != null) {
            log.debug("系统配置缓存命中: key={}", key);
            return cachedValue.toString();
        }

        // 2. 缓存未命中，查询数据库
        QueryWrapper<SysConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("config_key", key);
        queryWrapper.eq("is_enabled", 1); // 只查询启用的配置
        SysConfig config = this.getOne(queryWrapper);

        String value = defaultValue;
        if (config != null && config.getConfigValue() != null) {
            value = config.getConfigValue();
        }

        // 3. 写入缓存
        redisUtils.set(cacheKey, value, CacheConstant.CONFIG_TTL);
        log.debug("系统配置写入缓存: key={}, value={}", key, value);

        return value;
    }

    @Override
    public int getIntValue(String key, int defaultValue) {
        String value = getConfigValue(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public void deleteConfigCache(String key) {
        String cacheKey = CacheConstant.CONFIG_PREFIX + key;
        redisUtils.delete(cacheKey);
        log.info("系统配置缓存已删除: key={}", key);
    }
}
