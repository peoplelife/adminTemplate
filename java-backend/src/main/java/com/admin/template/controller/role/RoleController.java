package com.admin.template.controller.role;

import com.admin.template.service.role.RoleService;
import com.admin.template.common.response.Result;
import com.admin.template.common.web.TraceIdFilter;
import com.admin.template.dto.role.RoleCreateRequest;
import com.admin.template.dto.role.RolePageResponse;
import com.admin.template.dto.role.RoleResponse;
import com.admin.template.dto.role.RoleUpdateRequest;
import com.admin.template.dto.validation.SchemaInput;
import com.admin.template.security.Permission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

@Validated
@RestController
@RequestMapping("/v1/roles")
@Tag(name = "角色管理", description = "角色信息与角色菜单权限分配")
public class RoleController {
    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @Operation(operationId = "roleList", summary = "分页查询角色", description = "分页查询角色，可按角色名称或代码模糊匹配，并按状态筛选。")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @Permission("system:role:list")
    public Result<RolePageResponse> list(@Parameter(description = "页码，从 1 开始", example = "1",
                               schema = @Schema(type = "integer", minimum = "1", defaultValue = "1"))
                       @RequestParam(required = false) String page,
                       @Parameter(description = "每页条数，范围 1～100", example = "20",
                               schema = @Schema(type = "integer", minimum = "1", maximum = "100", defaultValue = "20"))
                       @RequestParam(required = false) String pageSize,
                       @Parameter(description = "角色名称或代码，模糊匹配", example = "管理员")
                       @RequestParam(required = false) String name,
                       @Parameter(description = "角色状态：0 禁用，1 启用", example = "1",
                               schema = @Schema(type = "integer", minimum = "0", maximum = "1"))
                       @RequestParam(required = false) String status,
                       @Parameter(hidden = true) @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(roleService.list(SchemaInput.queryInteger(page, "page", 1, 1, Integer.MAX_VALUE),
                SchemaInput.queryInteger(pageSize, "pageSize", 20, 1, 100), SchemaInput.optionalValue(name),
                SchemaInput.queryInteger(status, "status", null, 0, 1)), traceId);
    }

    @PostMapping
    @Operation(operationId = "roleCreate", summary = "创建角色", description = "创建角色并分配菜单权限，角色代码必须唯一。")
    @ApiResponse(responseCode = "201", description = "角色创建成功")
    @Permission("system:role:create")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<RoleResponse> create(@Valid @RequestBody RoleCreateRequest request,
            @Parameter(hidden = true) @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(roleService.create(request), "角色创建成功", traceId);
    }

    @GetMapping("/{id}")
    @Operation(operationId = "roleGet", summary = "查询角色详情", description = "根据角色数据库 ID 查询角色及已分配的菜单 ID。")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @Permission("system:role:list")
    public Result<RoleResponse> get(@Parameter(description = "角色数据库 ID，正整数", example = "1",
            schema = @Schema(type = "integer", format = "int64", minimum = "1")) @PathVariable String id,
            @Parameter(hidden = true) @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(roleService.get(SchemaInput.positiveId(id)), traceId);
    }

    @PatchMapping("/{id}")
    @Operation(operationId = "roleUpdate", summary = "修改角色", description = "只更新传入的字段；传入 menuIds 时替换该角色的菜单权限，空数组表示清空。")
    @ApiResponse(responseCode = "200", description = "角色修改成功")
    @Permission("system:role:update")
    public Result<RoleResponse> update(@Parameter(description = "角色数据库 ID，正整数", example = "1",
            schema = @Schema(type = "integer", format = "int64", minimum = "1")) @PathVariable String id,
                               @Valid @RequestBody RoleUpdateRequest request,
                               @Parameter(hidden = true) @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(roleService.update(SchemaInput.positiveId(id), request), "角色信息更新成功", traceId);
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "roleDelete", summary = "删除角色", description = "删除未被用户使用的角色；系统管理员角色不允许删除。成功后无响应体。")
    @ApiResponse(responseCode = "204", description = "角色已删除")
    @Permission("system:role:delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "角色数据库 ID，正整数", example = "1",
            schema = @Schema(type = "integer", format = "int64", minimum = "1")) @PathVariable String id) {
        roleService.delete(SchemaInput.positiveId(id));
    }
}
