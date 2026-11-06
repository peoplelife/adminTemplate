package com.admin.template.controller.user;

import com.admin.template.service.user.UserExportService;
import com.admin.template.service.user.UserService;
import com.admin.template.common.response.Result;
import com.admin.template.common.web.TraceIdFilter;
import com.admin.template.security.AuthenticatedUser;
import com.admin.template.security.Permission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.admin.template.dto.user.UserCreateRequest;
import com.admin.template.dto.user.UserDetail;
import com.admin.template.dto.user.UserPageResponse;
import com.admin.template.dto.user.UserResponse;
import com.admin.template.dto.user.UserUpdateRequest;
import com.admin.template.dto.validation.SchemaInput;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Validated
@RestController
@RequestMapping("/v1")
@Tag(name = "用户管理", description = "用户账号、角色和状态管理")
public class UserController {
    private final UserService userService;
    private final UserExportService exportService;

    public UserController(UserService userService, UserExportService exportService) {
        this.userService = userService;
        this.exportService = exportService;
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户", description = "获取当前登录账号的基本信息和角色编码。")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public Result<UserDetail> me(@AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(hidden = true) @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(detail(userService.getUserById(user.id())), traceId);
    }

    @GetMapping("/users")
    @Permission("system:user:list")
    @Operation(summary = "查询用户列表", description = "分页查询用户，用户名条件同时匹配用户名和昵称。需要用户列表权限。")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<UserPageResponse> list(@Parameter(description = "页码，从 1 开始", schema = @Schema(type = "integer", format = "int32", minimum = "1", maximum = "2147483647", defaultValue = "1"))
                       @RequestParam(required = false) String page,
                       @Parameter(description = "每页数量，范围 1 到 100", schema = @Schema(type = "integer", minimum = "1", maximum = "100", defaultValue = "20"))
                       @RequestParam(required = false) String pageSize,
                       @Parameter(description = "按用户名或昵称模糊查询", example = "alice")
                       @RequestParam(required = false) String username,
                       @Parameter(description = "用户状态：0 禁用，1 启用", schema = @Schema(type = "integer", minimum = "0", maximum = "1"))
                       @RequestParam(required = false) String status,
                       @Parameter(hidden = true) @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(userService.getUsers(SchemaInput.queryInteger(page, "page", 1, 1, Integer.MAX_VALUE),
                SchemaInput.queryInteger(pageSize, "pageSize", 20, 1, 100), SchemaInput.optionalValue(username),
                SchemaInput.queryInteger(status, "status", null, 0, 1)), traceId);
    }

    @GetMapping("/users/export")
    @Permission("system:user:export")
    @Operation(summary = "导出用户列表", description = "按条件导出 Excel 文件。需要用户导出权限。")
    @ApiResponse(responseCode = "200", description = "Excel 文件",
            content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    schema = @Schema(type = "string", format = "binary")))
    public ResponseEntity<byte[]> export(@Parameter(description = "按用户名或昵称模糊查询", example = "alice")
                                         @RequestParam(required = false) String username,
                                         @Parameter(description = "用户状态：0 禁用，1 启用", schema = @Schema(type = "integer", minimum = "0", maximum = "1"))
                                         @RequestParam(required = false) String status) {
        byte[] bytes = exportService.createUserListBuffer(userService.getExportData(SchemaInput.optionalValue(username),
                SchemaInput.queryInteger(status, "status", null, 0, 1)));
        String filename = URLEncoder.encode("用户列表-" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy_MM_dd HH_mm_ss")) + ".xlsx", StandardCharsets.UTF_8)
                .replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename);
        headers.setContentLength(bytes.length);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @PostMapping("/users")
    @Permission("system:user:create")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建用户", description = "创建账号并分配角色。需要用户创建权限。")
    @ApiResponse(responseCode = "201", description = "创建成功")
    public Result<UserDetail> create(@Valid @RequestBody UserCreateRequest request,
            @Parameter(hidden = true) @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(detail(userService.createUser(request)), "用户创建成功", traceId);
    }

    @GetMapping("/users/{id}")
    @Permission("system:user:list")
    @Operation(summary = "获取用户详情", description = "根据用户 ID 查询基本信息。需要用户列表权限。")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public Result<UserDetail> get(@Parameter(description = "用户 ID，必须为正整数", example = "1", required = true,
                                   schema = @Schema(type = "integer", format = "int64", minimum = "1"))
                                   @PathVariable String id,
                                   @Parameter(hidden = true) @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(detail(userService.getUserById(SchemaInput.positiveId(id))), traceId);
    }

    @PatchMapping("/users/{id}")
    @Permission("system:user:update")
    @Operation(summary = "更新用户", description = "按提供的字段更新用户；提供 roleCodes 时替换角色。需要用户更新权限。")
    @ApiResponse(responseCode = "200", description = "更新成功")
    public Result<UserDetail> update(@Parameter(description = "用户 ID，必须为正整数", example = "1", required = true,
                                      schema = @Schema(type = "integer", format = "int64", minimum = "1"))
                                      @PathVariable String id, @Valid @RequestBody UserUpdateRequest request,
                                      @Parameter(hidden = true) @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(detail(userService.updateUser(SchemaInput.positiveId(id), request)), "用户信息更新成功", traceId);
    }

    @DeleteMapping("/users/{id}")
    @Permission("system:user:delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除用户", description = "删除指定用户，不能删除当前登录账号。需要用户删除权限。")
    @ApiResponse(responseCode = "204", description = "删除成功", content = @Content)
    public void delete(@Parameter(description = "用户 ID，必须为正整数", example = "1", required = true,
                       schema = @Schema(type = "integer", format = "int64", minimum = "1"))
                       @PathVariable String id, @AuthenticationPrincipal AuthenticatedUser user) {
        userService.deleteUser(SchemaInput.positiveId(id), user.id());
    }

    private UserDetail detail(UserResponse user) {
        return new UserDetail(user.id(), user.username(), user.nickname(), user.email(), user.phone(),
                user.avatar(), user.roleCodes(), user.status());
    }
}
