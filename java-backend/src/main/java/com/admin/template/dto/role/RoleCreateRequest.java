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

public record RoleCreateRequest(
        @Schema(description = "唯一角色代码，仅允许字母、数字、下划线及连字符", example = "operator",
                requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]+") String code,
        @Schema(description = "角色名称", example = "运营人员", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String name,
        @Schema(description = "角色描述，默认空字符串", example = "负责日常运营", defaultValue = "") String description,
        @Schema(description = "角色状态：0 禁用，1 启用", minimum = "0", maximum = "1", defaultValue = "1")
        @Min(0) @Max(1) Integer status,
        @Schema(description = "授权菜单的业务 ID 列表，默认为空列表", example = "[\"menu_user\"]")
        List<@NotBlank String> menuIds) {
    public RoleCreateRequest {
        if (code != null) code = code.trim();
        if (name != null) name = name.trim();
        if (description == null) description = "";
        else description = description.trim();
        if (status == null) status = 1;
        if (menuIds == null) menuIds = List.of();
        menuIds = menuIds.stream().map(menuId -> menuId == null ? "null" : menuId.trim()).distinct().toList();
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static RoleCreateRequest fromJson(JsonNode body) {
        SchemaInput input = new SchemaInput(body);
        return new RoleCreateRequest(input.requiredString("code", Integer.MAX_VALUE), input.requiredString("name", Integer.MAX_VALUE),
                input.optionalString("description"), input.integer("status", 1, true, 0, 1), input.stringList("menuIds", List.of()));
    }
}
