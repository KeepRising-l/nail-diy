package com.diy.nail.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：所有异常统一转成 { code, message, data } 返回，绝不把堆栈抛给前端。
 * HTTP 状态码与业务 code 保持一致（4xx/5xx），网关与监控可据此识别失败请求；
 * 前端 request.js 的错误分支同样从 error.response.data 读取 message，用户提示不受影响。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：直接返回给用户的提示 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return build(e.getCode(), e.getMessage());
    }

    /** 参数校验异常（@Valid 表单/JSON） */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Result<Void>> handleValidException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError == null ? "参数错误" : fieldError.getDefaultMessage();
        return build(ResultCode.PARAM_ERROR.getCode(), message);
    }

    /** 请求参数类型/格式不合法：路径 id 传非数字、JSON 解析失败、缺少必填参数 */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<Result<Void>> handleBadRequest(Exception e) {
        log.warn("请求参数不合法: {}", e.getMessage());
        return build(ResultCode.PARAM_ERROR.getCode(), "请求参数不合法");
    }

    /** 请求路径不存在（含静态资源未命中） */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNotFound(NoResourceFoundException e) {
        log.warn("资源不存在: {}", e.getMessage());
        return build(ResultCode.NOT_FOUND.getCode(), "请求的资源不存在");
    }

    /** 兜底异常：内部错误，统一提示不暴露细节 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("系统异常", e);
        return build(ResultCode.ERROR.getCode(), ResultCode.ERROR.getMessage());
    }

    /** 业务 code → HTTP 状态：仅接受 4xx/5xx，其余一律归为 400，避免 200 掩盖失败 */
    private ResponseEntity<Result<Void>> build(Integer code, String message) {
        HttpStatus status = code == null ? null : HttpStatus.resolve(code);
        int httpCode = (status != null && status.isError()) ? code : ResultCode.PARAM_ERROR.getCode();
        return ResponseEntity.status(httpCode).body(Result.error(code, message));
    }
}
