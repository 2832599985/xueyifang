package com.xueyifang.common;

/**
 * 返回工具类
 * 
 * @author xueyifang
 */
public class ResultUtils {

    /**
     * 成功
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(ErrorCode.SUCCESS.getCode(), "success", data);
    }

    /**
     * 成功（无返回数据）
     */
    public static BaseResponse<Void> success() {
        return new BaseResponse<>(ErrorCode.SUCCESS.getCode(), "success", null);
    }

    /**
     * 成功（自定义消息，无返回数据）
     */
    public static BaseResponse<Void> success(String message) {
        return new BaseResponse<>(ErrorCode.SUCCESS.getCode(), message, null);
    }

    /**
     * 失败
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    /**
     * 失败
     */
    public static <T> BaseResponse<T> error(int code, String message) {
        return new BaseResponse<>(code, message);
    }

    /**
     * 失败
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode, String message) {
        return new BaseResponse<>(errorCode.getCode(), message);
    }
}

