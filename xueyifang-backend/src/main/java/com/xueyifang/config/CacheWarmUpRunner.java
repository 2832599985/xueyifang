package com.xueyifang.config;

import com.xueyifang.constant.CacheConstant;
import com.xueyifang.domain.po.Professional;
import com.xueyifang.domain.po.ServiceTag;
import com.xueyifang.domain.po.TradeLocation;
import com.xueyifang.service.IProfessionalService;
import com.xueyifang.service.IServiceTagService;
import com.xueyifang.service.ISysConfigService;
import com.xueyifang.service.ITradeLocationService;
import com.xueyifang.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 缓存预热 - 应用启动时自动预加载热点数据到 Redis
 *
 * <p>实现 CommandLineRunner 接口，在 Spring Boot 启动完成后执行。
 *
 * <h3>预热内容：</h3>
 * <ul>
 *   <li>服务标签列表（几乎每次搜索都会用到）</li>
 *   <li>专业列表（注册、个人信息页面使用）</li>
 *   <li>交易地点列表（发布服务、订单页面使用）</li>
 *   <li>核心系统配置（如 REVIEW_MODE）</li>
 * </ul>
 *
 * <h3>优势：</h3>
 * <ul>
 *   <li>首次请求不会触发数据库查询</li>
 *   <li>减少冷启动时的响应延迟</li>
 *   <li>答辩亮点：体现对系统性能的整体考虑</li>
 * </ul>
 *
 * @author xueyifang
 */
@Slf4j
@Component
public class CacheWarmUpRunner implements CommandLineRunner {

    @Resource
    private IServiceTagService serviceTagService;

    @Resource
    private IProfessionalService professionalService;

    @Resource
    private ITradeLocationService tradeLocationService;

    @Resource
    private ISysConfigService sysConfigService;

    @Resource
    private RedisUtils redisUtils;

    @Override
    public void run(String... args) {
        log.info("========== 开始缓存预热 ==========");

        long startTime = System.currentTimeMillis();

        try {
            // 1. 预热服务标签
            warmUpServiceTags();

            // 2. 预热专业列表
            warmUpProfessionals();

            // 3. 预热交易地点
            warmUpTradeLocations();

            // 4. 预热系统配置
            warmUpSysConfig();

        } catch (Exception e) {
            // 缓存预热失败不影响应用启动
            log.error("缓存预热过程中发生异常", e);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("========== 缓存预热完成，耗时 {}ms ==========", elapsed);
    }

    /**
     * 预热服务标签列表
     * 注意：必须使用 listAllServiceTags() 获取 ServiceTagVO 列表，
     * 以确保缓存格式与 ServiceTagServiceImpl 读取缓存时的格式一致（使用 tagId 字段）
     */
    private void warmUpServiceTags() {
        try {
            // 先删除旧缓存，确保获取最新数据（避免旧格式缓存导致字段不匹配）
            redisUtils.delete(CacheConstant.DICT_SERVICE_TAGS);
            // 使用 listAllServiceTags() 返回 ServiceTagVO 列表（包含 tagId 字段）
            // 该方法会查询数据库并重新设置正确格式的缓存
            List<com.xueyifang.domain.vo.ServiceTagVO> tags = serviceTagService.listAllServiceTags();
            log.info("预热服务标签列表成功: {} 条记录", tags != null ? tags.size() : 0);
        } catch (Exception e) {
            log.warn("预热服务标签列表失败: {}", e.getMessage());
        }
    }

    /**
     * 预热专业列表
     */
    private void warmUpProfessionals() {
        try {
            List<Professional> professionals = professionalService.list();
            if (professionals != null && !professionals.isEmpty()) {
                redisUtils.setJson(CacheConstant.DICT_PROFESSIONALS, professionals, CacheConstant.DICT_TTL_24H);
                log.info("预热专业列表成功: {} 条记录", professionals.size());
            }
        } catch (Exception e) {
            log.warn("预热专业列表失败: {}", e.getMessage());
        }
    }

    /**
     * 预热交易地点列表
     */
    private void warmUpTradeLocations() {
        try {
            List<TradeLocation> locations = tradeLocationService.list();
            if (locations != null && !locations.isEmpty()) {
                redisUtils.setJson(CacheConstant.DICT_TRADE_LOCATIONS, locations, CacheConstant.DICT_TTL_24H);
                log.info("预热交易地点列表成功: {} 条记录", locations.size());
            }
        } catch (Exception e) {
            log.warn("预热交易地点列表失败: {}", e.getMessage());
        }
    }

    /**
     * 预热核心系统配置
     */
    private void warmUpSysConfig() {
        try {
            // 预热 REVIEW_MODE 配置
            String reviewModeKey = CacheConstant.CONFIG_PREFIX + "REVIEW_MODE";
            int reviewMode = sysConfigService.getIntValue("REVIEW_MODE", 1);
            redisUtils.set(reviewModeKey, String.valueOf(reviewMode), CacheConstant.CONFIG_TTL);
            log.info("预热系统配置成功: REVIEW_MODE={}", reviewMode);
        } catch (Exception e) {
            log.warn("预热系统配置失败: {}", e.getMessage());
        }
    }
}
