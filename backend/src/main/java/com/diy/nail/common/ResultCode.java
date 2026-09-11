package com.diy.nail.common;

import lombok.Getter;

/**
 * 错误码常量（对应 HTTP 状态语义，由全局异常处理器转换为统一返回）
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    BUSINESS_ERROR(400, "业务处理失败"),
    ERROR(500, "系统异常");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
