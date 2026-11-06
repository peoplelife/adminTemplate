package com.admin.template.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "用户详情")
public record UserDetail(@Schema(description = "用户 ID", example = "1") long id,
                         @Schema(description = "用户名", example = "alice") String username,
                         @Schema(description = "昵称", example = "Alice") String nickname,
                         @Schema(description = "邮箱", example = "alice@example.com") String email,
                         @Schema(description = "手机号", example = "\"13800138000\"") String phone,
                         @Schema(description = "头像地址", example = "/uploads/avatar.png") String avatar,
                         @Schema(description = "角色编码列表") List<String> roleCodes,
                         @Schema(description = "状态：0 禁用，1 启用", minimum = "0", maximum = "1") int status) {
}
