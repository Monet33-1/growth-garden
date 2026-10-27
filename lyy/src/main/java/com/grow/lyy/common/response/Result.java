package com.grow.lyy.common.response;

public class Result<T> {

    private Integer code;

    private String message;

    private T data;

    public Result() {
    }
    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
    public static <T> Result<T> success() {
        return new Result<>(
                ResultCode.SUCCESS.getCode(),
                ResultCode.SUCCESS.getMessage(),
                null
        );
    }
    public static <T> Result<T> success(T data) {
        return new Result<>(
                ResultCode.SUCCESS.getCode(),
                ResultCode.SUCCESS.getMessage(),
                data
        );
    }
    public static <T> Result<T> fail() {
        return new Result<>(
                ResultCode.FAIL.getCode(),
                ResultCode.FAIL.getMessage(),
                null
        );
    }
    public static <T> Result<T> fail(String message) {
        return new Result<>(
                ResultCode.FAIL.getCode(),
                message,
                null
        );
    }
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(
                code,
                message,
                null
        );
    }
    public Integer getCode() {
        return code;
    }
    public void setCode(Integer code) {
        this.code = code;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public T getData() {
        return data;
    }
    public void setData(T data) {
        this.data = data;
    }
}