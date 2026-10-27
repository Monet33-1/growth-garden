package com.grow.lyy.common.response;

public enum ResultCode {

    SUCCESS(200, "success"),
    FAIL(500, "fail"),
    VALIDATE_FAILED(400, "参数错误"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "没有权限");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}