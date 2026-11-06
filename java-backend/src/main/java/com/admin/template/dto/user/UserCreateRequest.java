package com.admin.template.dto.user;

import com.admin.template.dto.validation.SchemaInput;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.util.List;

public record UserCreateRequest(@Schema(description = "用户名", example = "alice") @NotBlank String username,
                                @Schema(description = "密码，至少 6 位；省略时使用系统默认密码", format = "password") @Size(min = 6) String password,
                                @Schema(description = "昵称", example = "Alice") String nickname,
                                @Schema(description = "邮箱", example = "alice@example.com") @Email String email,
                                @Schema(description = "手机号，6 至 20 位数字", example = "\"13800138000\"") @Pattern(regexp = "\\d{6,20}") String phone,
                                @Schema(description = "头像地址", example = "/uploads/avatar.png") String avatar,
                                @Schema(description = "状态：0 禁用，1 启用", defaultValue = "1", minimum = "0", maximum = "1") @Min(0) @Max(1) Integer status,
                                @Schema(description = "分配的角色编码列表，省略时为空列表") List<@NotBlank String> roleCodes) {
    public UserCreateRequest {
        if (username != null) username = username.trim();
        password = password == null || password.isEmpty() ? null : password.trim();
        if (status == null) status = 1;
        if (nickname != null) nickname = nickname.trim();
        if (email != null) email = email.trim();
        if (phone != null) phone = phone.trim();
        if (avatar != null) avatar = avatar.trim();
        if (roleCodes == null) roleCodes = List.of();
        roleCodes = roleCodes.stream().map(code -> code == null ? "null" : code.trim()).distinct().toList();
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static UserCreateRequest fromJson(JsonNode body) {
        SchemaInput input = new SchemaInput(body);
        String password = input.optionalString("password");
        if (password != null && password.length() < 6) throw SchemaInput.invalid("password", "密码至少需要6位字符", "too_small");
        return new UserCreateRequest(input.requiredString("username", Integer.MAX_VALUE), password,
                input.optionalString("nickname"), input.email("email"), input.optionalString("phone"),
                input.optionalString("avatar"), input.integer("status", 1, true, 0, 1), input.stringList("roleCodes", List.of()));
    }
}
