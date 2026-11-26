package com.xueyifang.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用响应类
 * 
 * @author xueyifang
 */
@Data
public class BaseResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应码
     */
    private int code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    public BaseResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public BaseResponse(int code, String message) {
        this(code, message, null);
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public BaseResponse(ErrorCode errorCode, T data) {
        this(errorCode.getCode(), errorCode.getMessage(), data);
    }
}

