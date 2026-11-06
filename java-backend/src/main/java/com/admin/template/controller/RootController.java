package com.admin.template.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "服务状态", description = "公开的服务连通性接口")
public class RootController {
    @GetMapping("/")
    @Operation(summary = "检查服务连通性", description = "返回纯文本 Hello World!，无需登录。")
    @ApiResponse(responseCode = "200", description = "服务正常",
            content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Hello World!")))
    public String greeting() {
        return "Hello World!";
    }
}
