package com.xueyifang.enums;

import lombok.Getter;

/**
 * 文件存储类型枚举
 *
 * @author xueyifang
 */
@Getter
public enum FileStorageTypeEnum {

    /**
     * 本地存储
     */
    LOCAL("本地存储", "local"),

    /**
     * 阿里云OSS
     */
    OSS("阿里云OSS", "oss");

    private final String text;
    private final String value;

    FileStorageTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 值
     * @return 枚举
     */
    public static FileStorageTypeEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (FileStorageTypeEnum anEnum : FileStorageTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
