package com.admin.template.common.exception;

public class BizException extends RuntimeException {
    private final BusinessError error;
    private final Object details;

    public BizException(BusinessError error) {
        this(error, error.message(), null);
    }

    public BizException(BusinessError error, Object details) {
        this(error, error.message(), details);
    }

    public BizException(BusinessError error, String message, Object details) {
        super(message);
        this.error = error;
        this.details = details;
    }

    public BusinessError error() {
        return error;
    }

    public int code() {
        return error.code();
    }

    public Object details() {
        return details;
    }
}
