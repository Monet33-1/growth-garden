package com.grow.lyy.common.exception;

import com.grow.lyy.common.response.Result;
import com.grow.lyy.common.response.ResultCode;
import jakarta.validation.ConstraintViolationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDeniedException(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.fail(ResultCode.FORBIDDEN.getCode(), "管理员验证未通过，请重新验证"));
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.fail(e.getMessage());
    }

    /**
     * @RequestBody 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {

        FieldError fieldError = e.getBindingResult().getFieldError();

        String message = fieldError == null
                ? ResultCode.VALIDATE_FAILED.getMessage()
                : fieldError.getDefaultMessage();

        return Result.fail(ResultCode.VALIDATE_FAILED.getCode(), message);
    }

    /**
     * 表单参数校验异常
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {

        FieldError fieldError = e.getBindingResult().getFieldError();

        String message = fieldError == null
                ? ResultCode.VALIDATE_FAILED.getMessage()
                : fieldError.getDefaultMessage();

        return Result.fail(ResultCode.VALIDATE_FAILED.getCode(), message);
    }

    /**
     * URL参数校验异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(
            ConstraintViolationException e) {

        return Result.fail(
                ResultCode.VALIDATE_FAILED.getCode(),
                e.getMessage()
        );
    }

    /**
     * 未知异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {

        e.printStackTrace();

        return Result.fail(
                ResultCode.FAIL.getCode(),
                "服务器内部错误"
        );
    }

}
