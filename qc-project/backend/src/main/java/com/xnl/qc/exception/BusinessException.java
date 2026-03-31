package com.xnl.qc.exception;

/**
 * 业务异常（可预期的错误，如参数校验失败、数据不存在等）
 * 由全局异常处理器捕获，统一返回 HTTP 400
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
