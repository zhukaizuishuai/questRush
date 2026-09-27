package com.learn.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotRoleException;
import com.learn.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理（文档 5.1 GlobalExceptionConfig）：统一兜底，返回统一返回体。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：按 BizException 携带的 code/message/data 返回 */
    @ExceptionHandler(BizException.class)
    public Result<Object> handleBiz(BizException e) {
        return Result.fail(e.getCode(), e.getMessage(), e.getData());
    }

    /** 未登录（Sa-Token） */
    @ExceptionHandler(NotLoginException.class)
    public Result<Object> handleNotLogin(NotLoginException e) {
        return Result.fail(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
    }

    /** 角色不足（Sa-Token，/api/admin/** 非 admin 访问） */
    @ExceptionHandler(NotRoleException.class)
    public Result<Object> handleNotRole(NotRoleException e) {
        return Result.fail(ResultCode.FORBIDDEN.getCode(), ResultCode.FORBIDDEN.getMessage());
    }

    /** 参数校验失败（@Valid DTO） */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Object> handleValid(BindException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe == null ? ResultCode.PARAM_ERROR.getMessage() : fe.getDefaultMessage();
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Object> handleUnreadable(HttpMessageNotReadableException e) {
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), "请求体格式错误");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Object> handleUploadSize(MaxUploadSizeExceededException e) {
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), "文件大小超出限制");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Object> handleNoResource(NoResourceFoundException e) {
        return Result.fail(ResultCode.NOT_FOUND.getCode(), ResultCode.NOT_FOUND.getMessage());
    }

    /** 兜底 */
    @ExceptionHandler(Exception.class)
    public Result<Object> handleOther(Exception e) {
        log.error("unhandled exception", e);
        return Result.fail(ResultCode.SERVER_ERROR.getCode(), ResultCode.SERVER_ERROR.getMessage());
    }
}
