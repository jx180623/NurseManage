package com.xnl.qc.exception;

import com.xnl.qc.dto.Dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一将异常转换为标准 Result 格式返回给前端
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：参数错误、数据不存在等（400） */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)   // HTTP 层面返回 200，业务 code 区分
    public Result<Void> handleBusiness(BusinessException ex) {
        log.warn("业务异常: {}", ex.getMessage());
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    /** JSR-303 参数校验失败（@NotBlank 等）*/
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数校验失败: {}", msg);
        return Result.fail(400, "参数校验失败：" + msg);
    }

    /** 权限不足（Security Exception）*/
    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleSecurity(SecurityException ex) {
        log.warn("权限拒绝: {}", ex.getMessage());
        return Result.fail(403, ex.getMessage());
    }

    /** 参数类型/格式错误 */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleIllegalArg(IllegalArgumentException ex) {
        log.warn("非法参数: {}", ex.getMessage());
        return Result.fail(400, ex.getMessage());
    }

    /** 未知系统异常（500），记录完整堆栈 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleUnknown(Exception ex) {
        log.error("系统异常", ex);
        return Result.fail(500, "系统内部错误，请联系管理员");
    }
}
