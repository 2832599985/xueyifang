package com.xueyifang.exception;

import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * @author xueyifang
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<?>> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException: {}", e.getMessage(), e);
        return buildErrorResponse(e.getCode(), e.getMessage());
    }

    /**
     * 参数验证异常 - @Valid 注解验证失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<?>> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        log.error("MethodArgumentNotValidException", e);
        // 获取第一个验证失败的字段错误信息
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数验证失败";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResultUtils.error(ErrorCode.PARAMS_ERROR, message));
    }

    /**
     * 数据绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<BaseResponse<?>> bindExceptionHandler(BindException e) {
        log.error("BindException", e);
        // 获取第一个验证失败的字段错误信息
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数绑定失败";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResultUtils.error(ErrorCode.PARAMS_ERROR, message));
    }

    /**
     * 约束验证异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResponse<?>> constraintViolationExceptionHandler(ConstraintViolationException e) {
        log.error("ConstraintViolationException", e);
        // 获取第一个验证失败的约束错误信息
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResultUtils.error(ErrorCode.PARAMS_ERROR, message));
    }

    /**
     * 运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<BaseResponse<?>> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误"));
    }

    /**
     * 其他异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<?>> exceptionHandler(Exception e) {
        log.error("Exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误"));
    }

    private ResponseEntity<BaseResponse<?>> buildErrorResponse(int code, String message) {
        HttpStatus status = mapHttpStatus(code);
        return ResponseEntity.status(status).body(ResultUtils.error(code, message));
    }

    private HttpStatus mapHttpStatus(int code) {
        if (code == ErrorCode.USER_NOT_LOGIN.getCode()
                || code == ErrorCode.TOKEN_INVALID.getCode()
                || code == ErrorCode.TOKEN_EXPIRED.getCode()
                || code == ErrorCode.USER_PASSWORD_ERROR.getCode()) {
            return HttpStatus.UNAUTHORIZED;
        }

        if (code == ErrorCode.NO_AUTH_ERROR.getCode()
                || code == ErrorCode.FORBIDDEN_ERROR.getCode()
                || code == ErrorCode.PERMISSION_DENIED.getCode()
                || code == ErrorCode.USER_NO_PUBLISH_PERMISSION.getCode()
                || code == ErrorCode.USER_ACCOUNT_DISABLED.getCode()) {
            return HttpStatus.FORBIDDEN;
        }

        if (code == ErrorCode.NOT_FOUND_ERROR.getCode()
                || code == ErrorCode.USER_NOT_EXIST.getCode()
                || code == ErrorCode.SERVICE_NOT_EXIST.getCode()
                || code == ErrorCode.ORDER_NOT_EXIST.getCode()
                || code == ErrorCode.DISPUTE_NOT_EXIST.getCode()
                || code == ErrorCode.CHAT_MESSAGE_NOT_EXIST.getCode()
                || code == ErrorCode.CHAT_RECEIVER_NOT_EXIST.getCode()) {
            return HttpStatus.NOT_FOUND;
        }

        if (code == ErrorCode.RATE_LIMIT_EXCEEDED.getCode()) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }

        if (code == ErrorCode.DUPLICATE_SUBMIT.getCode()
                || code == ErrorCode.USER_STUDENT_ID_EXIST.getCode()) {
            return HttpStatus.CONFLICT;
        }

        if (code >= ErrorCode.SYSTEM_ERROR.getCode()) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return HttpStatus.BAD_REQUEST;
    }
}

