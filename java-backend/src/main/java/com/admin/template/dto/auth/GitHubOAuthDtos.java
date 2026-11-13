package com.admin.template.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class GitHubOAuthDtos {
    private GitHubOAuthDtos() {}

    @Schema(description = "GitHub 登录配置")
    public record Config(@Schema(description = "是否启用 GitHub 登录") boolean enabled) {}

    @Schema(description = "GitHub 登录、首次授权注册或账号绑定入口；state 由前端暂存用于完成授权")
    public record Authorization(@Schema(description = "固定 GitHub 授权地址，使用当前窗口跳转") String authorizationUrl,
                                @Schema(description = "五分钟有效的一次性状态值") String state) {}

    @Schema(description = "消费同一浏览器会话中的 GitHub 授权结果，完成登录、首次注册或账号绑定")
    public record CompleteRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}")
            @Schema(description = "发起授权时收到的 state", requiredMode = Schema.RequiredMode.REQUIRED,
                    minLength = 43, maxLength = 43) String state) {}

    @Schema(description = "当前用户的 GitHub 绑定状态")
    public record Binding(@Schema(description = "是否启用 GitHub 登录") boolean enabled,
                          @Schema(description = "是否已绑定 GitHub") boolean bound,
                          @Schema(description = "已绑定的 GitHub 登录名；未绑定时为 null", nullable = true) String login) {}
}
