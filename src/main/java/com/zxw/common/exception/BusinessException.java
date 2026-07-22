package com.zxw.common.exception;

/**
 * 业务异常。
 * 用于在业务流程中主动抛出可预期的错误，并携带对应的 HTTP 状态码。
 */
public class BusinessException extends RuntimeException {

    private final int status;

    /**
     * 使用默认 400 状态码创建业务异常。
     */
    public BusinessException(String message) {
        // 默认按 400 业务错误处理
        this(400, message);
    }

    /**
     * 使用指定状态码和消息创建业务异常。
     */
    public BusinessException(int status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * 返回业务异常对应的 HTTP 状态码。
     */
    public int getStatus() {
        return status;
    }
}
