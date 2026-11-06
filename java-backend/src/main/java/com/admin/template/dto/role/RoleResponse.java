package com.admin.template.dto.role;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record RoleResponse(
        @Schema(description = "角色数据库 ID", example = "1") long id,
        @Schema(description = "唯一角色代码", example = "operator") String code,
        @Schema(description = "角色名称", example = "运营人员") String name,
        @Schema(description = "角色描述", example = "负责日常运营") String description,
        @Schema(description = "角色状态：0 禁用，1 启用", minimum = "0", maximum = "1") int status,
        @Schema(description = "已授权菜单的业务 ID 列表", example = "[\"menu_user\"]") List<String> menuIds,
        @Schema(description = "创建时间（UTC）", example = "2026-10-27T10:00:00.000Z")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC") LocalDateTime createdAt,
        @Schema(description = "更新时间（UTC）", example = "2026-10-27T10:00:00.000Z")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC") LocalDateTime updatedAt) {
}
