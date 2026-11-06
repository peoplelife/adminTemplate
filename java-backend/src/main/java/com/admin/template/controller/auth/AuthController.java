package com.admin.template.controller.auth;

import com.admin.template.service.auth.AuthService;
import com.admin.template.dto.auth.LoginRequest;
import com.admin.template.dto.auth.LoginResponse;
import com.admin.template.common.response.Result;
import com.admin.template.common.web.TraceIdFilter;
import com.admin.template.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@Tag(name = "认证管理", description = "账号登录与退出登录")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(operationId = "authLogin", summary = "账号登录", description = "使用用户名和密码登录，成功后从 data.token 获取 Bearer Token。")
    @ApiResponse(responseCode = "200", description = "登录成功")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest body,
                                       @Parameter(hidden = true)
                                       @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(authService.login(body.username(), body.password()), "登录成功", traceId);
    }

    @PostMapping("/logout")
    @Operation(operationId = "authLogout", summary = "退出登录", description = "记录当前登录用户的退出时间，成功响应的 data 为 null。")
    @ApiResponse(responseCode = "200", description = "退出登录成功")
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> logout(@AuthenticationPrincipal AuthenticatedUser user,
                               @Parameter(hidden = true)
                               @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        authService.logout(user.id());
        return Result.ok(null, "退出登录成功", traceId);
    }
}
