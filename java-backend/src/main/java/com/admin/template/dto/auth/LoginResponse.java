package com.admin.template.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
        @Schema(description = "JWT 访问令牌，后续请求使用 Authorization: Bearer <token>") String token,
        @Schema(description = "是否首次登录", example = "false") boolean initialPasswordFlag) {
}
