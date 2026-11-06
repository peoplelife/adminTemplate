package com.admin.template.dto.menu;

import com.admin.template.dto.validation.SchemaInput;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MenuCreateRequest(
        @Schema(description = "菜单名称，最多 50 字符", example = "用户管理", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 50) String name,
        @Schema(description = "类型：M 目录，C 页面，B 按钮；兼容小写", allowableValues = {"M", "C", "B", "m", "c", "b"},
                requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Pattern(regexp = "(?i)M|C|B") String type,
        @Schema(description = "父菜单业务 ID；不传表示根菜单", example = "menu_system") String parentId,
        @Schema(description = "前端路由路径，最多 100 字符，默认空字符串", example = "/system/users")
        @Size(max = 100) String path,
        @Schema(description = "前端页面组件路径，最多 100 字符", example = "system/users/index")
        @Size(max = 100) String component,
        @Schema(description = "菜单图标，最多 50 字符", example = "user") @Size(max = 50) String icon,
        @Schema(description = "排序值，非负整数，默认 0", defaultValue = "0") @Min(0) Integer sort,
        @Schema(description = "是否隐藏：0 显示，1 隐藏", minimum = "0", maximum = "1", defaultValue = "0")
        @Min(0) @Max(1) Integer hidden,
        @Schema(description = "是否全屏：0 否，1 是", minimum = "0", maximum = "1", defaultValue = "0")
        @Min(0) @Max(1) Integer isFull,
        @Schema(description = "按钮权限标识，最多 100 字符", example = "system:user:list")
        @Size(max = 100) String buttonPermission) {
    public MenuCreateRequest {
        type = type == null ? null : type.trim().toUpperCase();
        name = name == null ? null : name.trim();
        parentId = parentId == null ? null : parentId.trim();
        path = path == null ? "" : path.trim();
        component = component == null ? "" : component.trim();
        icon = icon == null ? "" : icon.trim();
        if (sort == null) sort = 0;
        if (hidden == null) hidden = 0;
        if (isFull == null) isFull = 0;
        buttonPermission = buttonPermission == null ? "" : buttonPermission.trim();
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static MenuCreateRequest fromJson(JsonNode body) {
        SchemaInput input = new SchemaInput(body);
        return new MenuCreateRequest(input.requiredString("name", 50), input.menuType("type", true),
                input.parentId("parentId"), SchemaInput.defaultEmpty(input.optionalString("path")),
                SchemaInput.defaultEmpty(input.optionalString("component")), SchemaInput.defaultEmpty(input.optionalString("icon")),
                input.integer("sort", 0, false, 0, Integer.MAX_VALUE),
                input.integer("hidden", 0, false, 0, 1), input.integer("isFull", 0, false, 0, 1),
                SchemaInput.defaultEmpty(input.optionalString("buttonPermission")));
    }

    @Schema(hidden = true)
    @AssertTrue(message = "sort must be non-negative")
    public boolean isSortValueValid() {
        return sort == null || sort >= 0;
    }
}
