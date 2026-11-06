package com.admin.template.common.exception;

public enum BusinessError {
    VALIDATION_ERROR(100001, "请求参数不正确"),
    RATE_LIMITED(100002, "请求过于频繁，请稍后重试"),

    INVALID_CREDENTIALS(110001, "用户名或密码错误"),
    AUTH_TOKEN_INVALID(110002, "Token 无效或已过期"),
    ACCOUNT_DISABLED(110003, "账号已被禁用"),
    PERMISSION_DENIED(110004, "无权执行此操作"),
    AUTH_REQUIRED(110005, "未提供有效的 Bearer Token"),
    ACCOUNT_WITHOUT_ROLE(110006, "当前账号未分配角色或菜单权限，请联系管理员"),
    ACCOUNT_WITHOUT_PERMISSION(110007, "当前账号未分配菜单权限，请联系管理员"),

    USER_NOT_FOUND(120001, "用户不存在"),
    USERNAME_EXISTS(120002, "用户名已存在"),
    CANNOT_DELETE_SELF(120003, "不能删除当前登录账号"),

    ROLE_CODE_EXISTS(130001, "角色代码已存在"),
    ROLE_NOT_FOUND(130002, "角色不存在"),
    SYSTEM_ROLE_PROTECTED(130003, "系统管理员角色不能删除"),
    ROLE_IN_USE(130004, "角色正在使用，无法删除"),

    MENU_ID_EXISTS(140001, "菜单ID已存在"),
    MENU_NOT_FOUND(140002, "菜单不存在"),
    MENU_PARENT_NOT_FOUND(140003, "父菜单不存在"),
    MENU_HAS_CHILDREN(140004, "请先删除子菜单"),

    FILE_NOT_FOUND(150001, "文件不存在"),

    RAG_KNOWLEDGE_BASE_NOT_FOUND(160101, "知识库不存在或无权访问"),
    RAG_KNOWLEDGE_BASE_DISABLED(160102, "知识库已禁用"),
    RAG_KNOWLEDGE_BASE_EMPTY(160103, "知识库没有可用文档"),
    RAG_DOCUMENT_NOT_FOUND(160111, "文档不存在或无权访问"),
    RAG_DOCUMENT_INVALID(160112, "文档格式不支持或无法处理"),
    RAG_JOB_NOT_FOUND(160113, "处理任务不存在或无权访问"),
    RAG_CONVERSATION_NOT_FOUND(160121, "会话不存在或无权访问"),
    RAG_CONVERSATION_BLOCKED(160122, "会话绑定资源不允许继续问答"),
    RAG_GENERATION_IN_PROGRESS(160123, "同一会话已有生成中的请求"),
    RAG_DUPLICATE_REQUEST(160124, "相同发送请求已经处理"),
    RAG_REQUEST_CONFLICT(160125, "请求 ID 与内容冲突"),
    RAG_QUOTA_EXCEEDED(160126, "AI 请求频率或资源配额超过限制");

    private final int code;
    private final String message;

    BusinessError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}
