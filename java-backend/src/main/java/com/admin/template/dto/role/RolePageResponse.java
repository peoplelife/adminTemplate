package com.admin.template.dto.role;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record RolePageResponse(
        @Schema(description = "当前页的角色列表") List<RoleResponse> list,
        @Schema(description = "分页信息") Pagination pagination) {
    @Schema(name = "RolePagination", description = "角色分页信息")
    public record Pagination(
            @Schema(description = "当前页码，从 1 开始", example = "1") int page,
            @Schema(description = "每页条数", example = "20") int pageSize,
            @Schema(description = "符合条件的角色总数", example = "42") long total,
            @Schema(description = "总页数", example = "3") int totalPages) {
    }
}
