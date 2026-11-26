package com.xueyifang.constant;

/**
 * Redis 缓存常量
 *
 * @author xueyifang
 */
public class CacheConstant {

    // ==================== 系统配置 ====================
    /**
     * 系统配置缓存前缀
     */
    public static final String CONFIG_PREFIX = "config:";
    /**
     * 系统配置缓存TTL：24小时
     */
    public static final long CONFIG_TTL = 86400;

    // ==================== 公共字典数据 ====================
    /**
     * 服务标签列表缓存Key
     */
    public static final String DICT_SERVICE_TAGS = "dict:service_tags";
    /**
     * 专业列表缓存Key
     */
    public static final String DICT_PROFESSIONALS = "dict:professionals";
    /**
     * 交易地点列表缓存Key
     */
    public static final String DICT_TRADE_LOCATIONS = "dict:trade_locations";
    /**
     * 字典缓存TTL：12小时
     */
    public static final long DICT_TTL_12H = 43200;
    /**
     * 字典缓存TTL：24小时
     */
    public static final long DICT_TTL_24H = 86400;

    // ==================== 服务详情 ====================
    /**
     * 服务详情缓存前缀
     */
    public static final String SERVICE_DETAIL_PREFIX = "service:detail:";
    /**
     * 服务详情缓存TTL：30分钟
     */
    public static final long SERVICE_DETAIL_TTL = 1800;

    // ==================== 卖家评分 ====================
    /**
     * 卖家评分缓存前缀
     */
    public static final String SELLER_RATING_PREFIX = "seller:rating:";
    /**
     * 卖家评分缓存TTL：1小时
     */
    public static final long SELLER_RATING_TTL = 3600;

    // ==================== 统计数据 ====================
    /**
     * 用户销售统计缓存前缀
     */
    public static final String STATISTICS_USER_SALES_PREFIX = "statistics:user:sales:";
    /**
     * 用户销售统计缓存TTL：5分钟
     */
    public static final long STATISTICS_USER_SALES_TTL = 300;
    /**
     * 管理员系统统计缓存Key
     */
    public static final String STATISTICS_ADMIN_SYSTEM = "statistics:admin:system";
    /**
     * 管理员系统统计缓存TTL：10分钟
     */
    public static final long STATISTICS_ADMIN_SYSTEM_TTL = 600;
    /**
     * 管理员趋势统计缓存Key
     */
    public static final String STATISTICS_ADMIN_TREND = "statistics:admin:trend";
    /**
     * 管理员趋势统计缓存TTL：5分钟
     */
    public static final long STATISTICS_ADMIN_TREND_TTL = 300;

    private CacheConstant() {
        // 私有构造函数，防止实例化
    }
}
