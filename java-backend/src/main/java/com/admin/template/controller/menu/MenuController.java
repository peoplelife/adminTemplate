package com.admin.template.controller.menu;

import com.admin.template.service.menu.MenuService;
import com.admin.template.common.response.Result;
import com.admin.template.common.web.TraceIdFilter;
import com.admin.template.dto.menu.MenuCreateRequest;
import com.admin.template.dto.menu.MenuListResponse;
import com.admin.template.dto.menu.MenuResponse;
import com.admin.template.dto.menu.MenuUpdateRequest;
import com.admin.template.dto.validation.SchemaInput;
import com.admin.template.security.AuthenticatedUser;
import com.admin.template.security.Permission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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


@RestController
@RequestMapping("/v1")
@Tag(name = "菜单管理", description = "菜单、页面与按钮权限")
public class MenuController {
    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/me/menus")
    @Operation(operationId = "menuCurrentUser", summary = "查询当前用户菜单", description = "返回当前用户可访问的菜单与按钮权限列表，列表中的 id 不返回。")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<MenuListResponse> me(@AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(hidden = true) @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(new MenuListResponse(menuService.userMenus(user.id())), traceId);
    }

    @GetMapping("/menus")
    @Operation(operationId = "menuList", summary = "查询菜单列表", description = "查询有效菜单，可按名称模糊匹配和类型筛选，按排序值升序返回。")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @Permission("system:menu:list")
    public Result<MenuListResponse> list(
            @Parameter(description = "菜单名称，模糊匹配", example = "用户") @RequestParam(required = false) String menuName,
            @Parameter(description = "菜单类型：M 目录，C 页面，B 按钮；兼容小写",
                    schema = @Schema(allowableValues = {"M", "C", "B", "m", "c", "b"}))
            @RequestParam(required = false) String menuType,
            @Parameter(hidden = true) @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(new MenuListResponse(menuService.list(SchemaInput.optionalValue(menuName),
                SchemaInput.queryMenuType(menuType))), traceId);
    }

    @PostMapping("/menus")
    @Operation(operationId = "menuCreate", summary = "创建菜单", description = "创建目录、页面或按钮，菜单业务 ID 由服务端生成。")
    @ApiResponse(responseCode = "201", description = "菜单创建成功")
    @Permission("system:menu:create")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<MenuResponse> create(@Valid @RequestBody MenuCreateRequest request,
            @Parameter(hidden = true) @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(menuService.create(request), "菜单创建成功", traceId);
    }

    @PatchMapping("/menus/{id}")
    @Operation(operationId = "menuUpdate", summary = "修改菜单", description = "至少传入一个待更新字段；路径参数使用数据库 ID，parentId 使用菜单业务 ID。")
    @ApiResponse(responseCode = "200", description = "菜单修改成功")
    @Permission("system:menu:update")
    public Result<MenuResponse> update(@Parameter(description = "菜单数据库 ID，正整数", example = "1",
            schema = @Schema(type = "integer", format = "int64", minimum = "1")) @PathVariable String id,
                               @Valid @RequestBody MenuUpdateRequest request,
                               @Parameter(hidden = true) @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(menuService.update(SchemaInput.positiveId(id), request), "菜单信息更新成功", traceId);
    }

    @DeleteMapping("/menus/{id}")
    @Operation(operationId = "menuDelete", summary = "删除菜单", description = "停用菜单；存在有效子菜单时不允许删除。成功后无响应体。")
    @ApiResponse(responseCode = "204", description = "菜单已删除")
    @Permission("system:menu:delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "菜单数据库 ID，正整数", example = "1",
            schema = @Schema(type = "integer", format = "int64", minimum = "1")) @PathVariable String id) {
        menuService.delete(SchemaInput.positiveId(id));
    }
}
