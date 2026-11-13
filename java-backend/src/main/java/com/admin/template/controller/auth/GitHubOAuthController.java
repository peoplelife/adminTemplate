package com.admin.template.controller.auth;

import com.admin.template.common.response.Result;
import com.admin.template.common.web.TraceIdFilter;
import com.admin.template.config.GitHubOAuthProperties;
import com.admin.template.dto.auth.GitHubOAuthDtos.*;
import com.admin.template.dto.auth.LoginResponse;
import com.admin.template.security.AuthenticatedUser;
import com.admin.template.service.auth.GitHubOAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "GitHub 认证", description = "已有账号的 GitHub 登录、绑定与解绑")
public class GitHubOAuthController {
    private final GitHubOAuthService github;
    private final GitHubOAuthProperties properties;

    public GitHubOAuthController(GitHubOAuthService github, GitHubOAuthProperties properties) {
        this.github = github;
        this.properties = properties;
    }

    @ModelAttribute
    void preventCredentialCaching(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
    }

    @GetMapping("/v1/auth/github/config")
    @Operation(operationId = "githubConfig", summary = "查询 GitHub 登录是否启用")
    public Result<Config> config(@Parameter(hidden = true)
                                 @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(new Config(properties.enabled()), traceId);
    }

    @PostMapping("/v1/auth/github/authorize")
    @Operation(operationId = "githubAuthorize", summary = "发起 GitHub 登录或注册", description = "返回授权地址和 state；同一浏览器会话五分钟内有效，首次授权将自动注册普通用户。")
    public Result<Authorization> authorize(HttpServletRequest request, @Parameter(hidden = true)
                                           @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(github.authorize(newFlowSession(request), null), traceId);
    }

    @PostMapping("/v1/me/github/authorize")
    @Operation(operationId = "githubBindingAuthorize", summary = "发起当前账号的 GitHub 绑定")
    public Result<Authorization> authorizeBinding(@AuthenticationPrincipal AuthenticatedUser user,
                                                  HttpServletRequest request, @Parameter(hidden = true)
                                                  @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(github.authorize(newFlowSession(request), user.id()), traceId);
    }

    @GetMapping("/v1/auth/github/callback")
    @Operation(operationId = "githubCallback", summary = "GitHub 授权回调", description = "由 GitHub 调用；结果暂存服务端，跳转到固定前端地址，URL 不携带令牌或授权码。")
    @ApiResponse(responseCode = "302", description = "跳转固定前端回调页")
    public ResponseEntity<Void> callback(HttpServletRequest request,
            @Parameter(description = "GitHub 一次性授权码") @RequestParam(required = false) String code,
            @Parameter(description = "原授权 state") @RequestParam(required = false) String state,
            @Parameter(description = "GitHub 授权错误") @RequestParam(required = false) String error) {
        properties.requireEnabled();
        github.callback(request.getSession(false), code, state, error);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(properties.frontendRedirectUri())).build();
    }

    @PostMapping("/v1/auth/github/complete")
    @Operation(operationId = "githubComplete", summary = "完成 GitHub 登录或注册", description = "使用原 state 消费同一浏览器会话的授权结果；首次授权自动注册无密码的普通用户，已有绑定登录原账号，成功返回现有 JWT 登录响应。")
    public Result<LoginResponse> complete(@Valid @RequestBody CompleteRequest body, HttpServletRequest request,
                                          @Parameter(hidden = true) @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(github.completeLogin(request.getSession(false), body.state()), "登录成功", traceId);
    }

    @PostMapping("/v1/me/github/complete")
    @Operation(operationId = "githubBindingComplete", summary = "完成当前账号的 GitHub 绑定", description = "当前 Bearer Token 用户必须与发起绑定时相同。")
    public Result<Binding> completeBinding(@AuthenticationPrincipal AuthenticatedUser user,
                                           @Valid @RequestBody CompleteRequest body, HttpServletRequest request,
                                           @Parameter(hidden = true) @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(github.completeBinding(request.getSession(false), body.state(), user.id()), traceId);
    }

    @GetMapping("/v1/me/github")
    @Operation(operationId = "githubBinding", summary = "查看当前账号的 GitHub 绑定")
    public Result<Binding> binding(@AuthenticationPrincipal AuthenticatedUser user, @Parameter(hidden = true)
                                   @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(github.binding(user.id()), traceId);
    }

    @DeleteMapping("/v1/me/github")
    @Operation(operationId = "githubUnbind", summary = "解除当前账号的 GitHub 绑定", description = "仅支持 GitHub 登录的无密码账号不能解绑，返回业务码 110013；已有密码账号可正常解绑。")
    @ApiResponse(responseCode = "204", description = "解绑成功，无响应体")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unbind(@AuthenticationPrincipal AuthenticatedUser user) {
        github.unbind(user.id());
    }

    private HttpSession newFlowSession(HttpServletRequest request) {
        properties.requireEnabled();
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        return session;
    }
}
