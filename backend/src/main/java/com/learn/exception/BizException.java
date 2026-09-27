package com.learn.exception;

import lombok.Getter;

/**
 * 业务异常：code + message（+ 可选 data，如登录失败时返回 requireCaptcha）
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;
    private final transient Object data;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
        this.data = null;
    }

    public BizException(int code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.data = null;
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
        this.data = null;
    }

    public BizException(ResultCode resultCode, String message, Object data) {
        super(message);
        this.code = resultCode.getCode();
        this.data = data;
    }

    public BizException(ResultCode resultCode, Object data) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.data = data;
    }
}
