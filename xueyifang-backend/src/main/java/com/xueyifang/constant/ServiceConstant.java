package com.xueyifang.constant;

/**
 * 服务常量
 * 
 * @author xueyifang
 */
public interface ServiceConstant {

    /**
     * 服务状态
     */
    int STATUS_ONLINE = 1;
    int STATUS_OFFLINE = 2;
    int STATUS_PENDING = 3;
    int STATUS_REJECTED = 4;

    /**
     * 审核状态
     */
    int REVIEW_APPROVED = 1;
    int REVIEW_PENDING = 2;
    int REVIEW_REJECTED = 3;

    /**
     * 交易类型
     */
    int TRADE_TYPE_OFFLINE = 1;
    int TRADE_TYPE_ONLINE = 2;

    /**
     * 是否实体物品
     */
    int NOT_PHYSICAL = 0;
    int IS_PHYSICAL = 1;

    /**
     * 排序方式
     */
    String SORT_CREATE_DESC = "create_desc";
    String SORT_PRICE_ASC = "price_asc";
    String SORT_PRICE_DESC = "price_desc";
    String SORT_VIEW_DESC = "view_desc";
    String SORT_LATEST = "latest";
}

