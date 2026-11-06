package com.admin.template.dto.role;

import com.admin.template.dto.validation.SchemaInput;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.util.List;

public record RoleUpdateRequest(
        @Schema(description = "唯一角色代码，仅允许字母、数字、下划线及连字符", example = "operator")
        @Pattern(regexp = "[a-zA-Z0-9_-]+") String code,
        @Schema(description = "角色名称", example = "运营人员") String name,
        @Schema(description = "角色描述", example = "负责日常运营") String description,
        @Schema(description = "角色状态：0 禁用，1 启用", minimum = "0", maximum = "1")
        @Min(0) @Max(1) Integer status,
        @Schema(description = "替换角色菜单权限的业务 ID 列表，空列表表示清空", example = "[\"menu_user\"]")
        List<@NotBlank String> menuIds) {
    public RoleUpdateRequest {
        if (code != null) code = code.trim();
        if (name != null) name = name.trim();
        if (description != null) description = description.trim();
        if (menuIds != null) menuIds = menuIds.stream().map(menuId -> menuId == null ? "null" : menuId.trim()).distinct().toList();
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static RoleUpdateRequest fromJson(JsonNode body) {
        SchemaInput input = new SchemaInput(body);
        return new RoleUpdateRequest(input.optionalString("code"), input.optionalString("name"),
                input.nullableString("description", Integer.MAX_VALUE), input.integer("status", null, false, 0, 1),
                input.stringList("menuIds", null));
    }
}
