package com.admin.template.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(description = "登录用户名", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String username,
        @Schema(description = "登录密码", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String password) {
    public LoginRequest {
        if (username != null) username = username.trim();
        if (password != null) password = password.trim();
    }
}
