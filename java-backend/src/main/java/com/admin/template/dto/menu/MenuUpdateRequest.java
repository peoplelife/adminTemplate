package com.admin.template.dto.menu;

import com.admin.template.dto.validation.SchemaInput;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

public record MenuUpdateRequest(
        @Schema(description = "菜单业务 ID，1～50 字符", example = "menu_user")
        @Size(min = 1, max = 50) String menuId,
        @Schema(description = "父菜单业务 ID；传 null 表示根菜单", example = "menu_system") String parentId,
        @Schema(description = "菜单名称，1～50 字符", example = "用户管理") @Size(min = 1, max = 50) String name,
        @Schema(description = "前端路由路径，最多 100 字符，可传 null", example = "/system/users")
        @Size(max = 100) String path,
        @Schema(description = "前端页面组件路径，最多 100 字符，可传 null", example = "system/users/index")
        @Size(max = 100) String component,
        @Schema(description = "菜单图标，最多 50 字符，可传 null", example = "user") @Size(max = 50) String icon,
        @Schema(description = "排序值，非负整数", example = "0") @Min(0) Integer sort,
        @Schema(description = "是否隐藏：0 显示，1 隐藏", minimum = "0", maximum = "1")
        @Min(0) @Max(1) Integer hidden,
        @Schema(description = "是否全屏：0 否，1 是", minimum = "0", maximum = "1")
        @Min(0) @Max(1) Integer isFull,
        @Schema(description = "类型：M 目录，C 页面，B 按钮；兼容小写", allowableValues = {"M", "C", "B", "m", "c", "b"})
        @Pattern(regexp = "(?i)M|C|B") String type,
        @Schema(description = "按钮权限标识，最多 100 字符，可传 null", example = "system:user:list")
        @Size(max = 100) String buttonPermission,
        @JsonIgnore Set<String> suppliedFields) {
    public MenuUpdateRequest(String menuId, String parentId, String name, String path, String component,
                             String icon, Integer sort, Integer hidden, Integer isFull, String type,
                             String buttonPermission) {
        this(menuId, parentId, name, path, component, icon, sort, hidden, isFull, type, buttonPermission,
                nonNullFields(menuId, parentId, name, path, component, icon, sort, hidden, isFull, type, buttonPermission));
    }

    public MenuUpdateRequest {
        type = type == null ? null : type.trim().toUpperCase();
        if (menuId != null) menuId = menuId.trim();
        if (parentId != null) parentId = parentId.trim();
        if (name != null) name = name.trim();
        if (path != null) path = path.trim();
        if (component != null) component = component.trim();
        if (icon != null) icon = icon.trim();
        if (buttonPermission != null) buttonPermission = buttonPermission.trim();
        suppliedFields = suppliedFields == null ? Set.of() : Set.copyOf(suppliedFields);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static MenuUpdateRequest fromJson(JsonNode body) {
        SchemaInput input = new SchemaInput(body);
        String type = input.menuType("type", false);
        Set<String> supplied = new HashSet<>();
        for (String field : new String[]{"menuId", "parentId", "name", "path", "component", "icon", "sort", "hidden", "isFull", "buttonPermission"}) {
            if (input.has(field)) supplied.add(field);
        }
        if (type != null) supplied.add("type");
        if (supplied.isEmpty()) throw SchemaInput.invalid("", "至少提供一个要更新的字段", "custom");
        return new MenuUpdateRequest(input.patchRequiredString("menuId", 50), input.parentId("parentId"),
                input.patchRequiredString("name", 50), input.nullableString("path", 100),
                input.nullableString("component", 100), input.nullableString("icon", 50),
                input.integer("sort", null, false, 0, Integer.MAX_VALUE), input.integer("hidden", null, false, 0, 1),
                input.integer("isFull", null, false, 0, 1), type, input.nullableString("buttonPermission", 100), supplied);
    }

    public boolean hasField(String field) {
        return suppliedFields.contains(field);
    }

    @Schema(hidden = true)
    @AssertTrue(message = "至少提供一个要更新的字段")
    public boolean isPatchNonEmpty() {
        return !suppliedFields.isEmpty();
    }

    private static Set<String> nonNullFields(Object... values) {
        String[] names = {"menuId", "parentId", "name", "path", "component", "icon", "sort", "hidden", "isFull", "type", "buttonPermission"};
        Set<String> fields = new HashSet<>();
        for (int index = 0; index < values.length; index++) if (values[index] != null) fields.add(names[index]);
        return fields;
    }
}
