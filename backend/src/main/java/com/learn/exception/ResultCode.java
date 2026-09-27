package com.learn.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举（文档 6.0）：0 / 401 / 403 / 40301 / 404 / 422 / 500
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    OK(0, "ok"),

    UNAUTHORIZED(401, "未登录或登录已过期"),
    /** 账号被禁用 / 逻辑删除：同 401，强制登出（文档 4.1） */
    ACCOUNT_DISABLED(401, "账号已被禁用或已删除"),

    FORBIDDEN(403, "无权限访问"),
    /** VIP 资源鉴权失败专用码（文档 4.1） */
    VIP_REQUIRED(40301, "该题目为VIP专属，请开通会员后查看"),

    NOT_FOUND(404, "资源不存在或已下架"),
    PARAM_ERROR(422, "参数校验失败"),
    SERVER_ERROR(500, "服务器繁忙，请稍后重试");

    private final int code;
    private final String message;
}
