package com.xueyifang.enums;

import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件上传业务类型枚举
 *
 * @author xueyifang
 */
@Getter
public enum FileUploadBizEnum {

    /**
     * 用户头像
     */
    USER_AVATAR("用户头像", "user_avatar", 1048576L),

    /**
     * 服务图片
     */
    SERVICE_IMAGE("服务图片", "service_image", 5242880L),

    /**
     * 订单履约交付凭证图片
     */
    DELIVERY_IMAGE("交付凭证图片", "delivery_image", 5242880L),

    /**
     * 平台纠纷举证图片
     */
    DISPUTE_EVIDENCE("纠纷举证图片", "dispute_evidence", 5242880L),

    /**
     * 站内私信图片
     */
    CHAT_IMAGE("聊天图片", "chat_image", 5242880L);

    private final String text;
    private final String value;
    private final Long maxSize;

    FileUploadBizEnum(String text, String value, Long maxSize) {
        this.text = text;
        this.value = value;
        this.maxSize = maxSize;
    }

    /**
     * 获取值列表
     *
     * @return 值列表
     */
    public static List<String> getValues() {
        return Arrays.stream(values()).map(FileUploadBizEnum::getValue).collect(Collectors.toList());
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 值
     * @return 枚举
     */
    public static FileUploadBizEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (FileUploadBizEnum anEnum : FileUploadBizEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
