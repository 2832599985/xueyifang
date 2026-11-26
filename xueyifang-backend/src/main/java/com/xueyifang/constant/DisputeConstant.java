package com.xueyifang.constant;

/**
 * 纠纷常量
 *
 * @author xueyifang
 * @date 2025-11-21
 */
public interface DisputeConstant {

    // ==================== 纠纷类型 ====================

    /**
     * 卖家未履约
     */
    Integer TYPE_SELLER_NOT_FULFILL = 1;

    /**
     * 货不对板
     */
    Integer TYPE_MISMATCH = 2;

    /**
     * 质量问题
     */
    Integer TYPE_QUALITY = 3;

    /**
     * 其他
     */
    Integer TYPE_OTHER = 4;

    // ==================== 纠纷状态 ====================

    /**
     * 待审核/待处理
     */
    Integer STATUS_PENDING = 1;

    /**
     * 处理中
     */
    Integer STATUS_PROCESSING = 2;

    /**
     * 已解决
     */
    Integer STATUS_RESOLVED = 3;

    /**
     * 已拒绝
     */
    Integer STATUS_REJECTED = 4;
}
