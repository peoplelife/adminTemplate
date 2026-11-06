package com.admin.template.common.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "统一接口响应")
public record Result<T>(
        @Schema(description = "数字结果码；业务成功固定为 0", type = "integer", format = "int32", example = "0") int code,
        @Schema(description = "成功时的业务数据；失败时为 null") T data,
        @Schema(description = "结果说明", example = "操作成功") String message,
        @Schema(description = "操作是否成功", example = "true") boolean success,
        @Schema(description = "请求追踪 ID；与 X-Trace-Id 响应头一致", example = "request-123") String traceId,
        @Schema(description = "响应时间（UTC）", example = "2026-10-27T08:00:00Z")
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp,
        @Schema(description = "失败时的错误详情；成功时为 null") Object details) {
    public static final String DEFAULT_SUCCESS_MESSAGE = "操作成功";

    public static <T> Result<T> ok(T data, String traceId) {
        return ok(data, DEFAULT_SUCCESS_MESSAGE, traceId);
    }

    public static <T> Result<T> ok(T data, String message, String traceId) {
        return new Result<>(0, data, message, true, traceId, Instant.now(), null);
    }

    public static <T> Result<T> fail(int code, String message, Object details, String traceId) {
        return new Result<>(code, null, message, false, traceId, Instant.now(), details);
    }
}
