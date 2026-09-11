package com.diy.nail.common;

import lombok.Getter;

/**
 * 业务异常：携带给用户看的（中文）提示信息，由全局异常处理器捕获并转成统一返回
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.BUSINESS_ERROR.getCode();
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
