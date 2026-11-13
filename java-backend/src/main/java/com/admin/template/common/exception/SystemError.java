package com.admin.template.common.exception;

import org.springframework.http.HttpStatus;

public enum SystemError {
    INTERNAL_ERROR(500000, HttpStatus.INTERNAL_SERVER_ERROR, "服务内部错误"),
    FILE_UPLOAD_FAILED(150101, HttpStatus.INTERNAL_SERVER_ERROR, "文件上传失败"),
    FILE_DELETE_FAILED(150102, HttpStatus.INTERNAL_SERVER_ERROR, "文件删除失败"),
    AI_SERVICE_ERROR(160001, HttpStatus.BAD_GATEWAY, "AI 服务暂时不可用"),
    RAG_EMBEDDING_ERROR(160501, HttpStatus.BAD_GATEWAY, "Embedding 服务暂时不可用"),
    RAG_GENERATION_ERROR(160502, HttpStatus.BAD_GATEWAY, "生成模型服务暂时不可用"),
    GITHUB_SERVICE_ERROR(170001, HttpStatus.BAD_GATEWAY, "GitHub 服务暂时不可用，请重新发起授权");

    private final int code;
    private final HttpStatus status;
    private final String message;

    SystemError(int code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
