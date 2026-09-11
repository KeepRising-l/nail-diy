package com.diy.nail.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结构：{ code, message, data }
 */
@Data
public class Result<T> implements Serializable {

    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> ok() {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> ok(T data) {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> ok(String message, T data) {
        return build(ResultCode.SUCCESS.getCode(), message, data);
    }

    public static <T> Result<T> error(ResultCode resultCode) {
        return build(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> Result<T> error(ResultCode resultCode, String message) {
        return build(resultCode.getCode(), message, null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return build(code, message, null);
    }

    private static <T> Result<T> build(Integer code, String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        return result;
    }
}
