package com.admin.template.dto.user;

import com.admin.template.dto.validation.SchemaInput;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.util.List;

public record UserUpdateRequest(@Schema(description = "用户名", example = "alice") String username,
                                @Schema(description = "昵称", example = "Alice") String nickname,
                                @Schema(description = "邮箱", example = "alice@example.com") @Email String email,
                                @Schema(description = "手机号，6 至 20 位数字", example = "\"13800138000\"") @Pattern(regexp = "\\d{6,20}") String phone,
                                @Schema(description = "头像地址", example = "/uploads/avatar.png") String avatar,
                                @Schema(description = "状态：0 禁用，1 启用", minimum = "0", maximum = "1") @Min(0) @Max(1) Integer status,
                                @Schema(description = "角色编码列表；提供时替换现有角色") List<@NotBlank String> roleCodes) {
    public UserUpdateRequest {
        if (username != null) username = username.trim();
        if (nickname != null) nickname = nickname.trim();
        if (email != null) email = email.trim();
        if (phone != null) phone = phone.trim();
        if (avatar != null) avatar = avatar.trim();
        if (roleCodes != null) roleCodes = roleCodes.stream().map(code -> code == null ? "null" : code.trim()).distinct().toList();
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static UserUpdateRequest fromJson(JsonNode body) {
        SchemaInput input = new SchemaInput(body);
        return new UserUpdateRequest(input.optionalString("username"), input.optionalString("nickname"),
                input.email("email"), input.optionalString("phone"), input.optionalString("avatar"),
                input.integer("status", null, false, 0, 1), input.stringList("roleCodes", null));
    }
}
