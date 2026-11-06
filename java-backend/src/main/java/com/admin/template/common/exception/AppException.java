package com.admin.template.common.exception;

public class AppException extends RuntimeException {
    private final SystemError error;
    private final Object details;

    public AppException(SystemError error, Object details, Throwable cause) {
        super(error.message(), cause);
        this.error = error;
        this.details = details;
    }

    public SystemError error() {
        return error;
    }

    public int code() {
        return error.code();
    }

    public Object details() {
        return details;
    }
}
