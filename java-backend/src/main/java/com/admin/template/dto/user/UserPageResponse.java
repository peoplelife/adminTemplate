package com.admin.template.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record UserPageResponse(@Schema(description = "用户列表") List<UserResponse> list,
                               @Schema(description = "分页信息") Pagination pagination) {
    public record Pagination(@Schema(description = "当前页码", example = "1") int page,
                             @Schema(description = "每页数量", example = "20") int pageSize,
                             @Schema(description = "记录总数", example = "100") long total,
                             @Schema(description = "总页数", example = "5") int totalPages) {
    }
}
