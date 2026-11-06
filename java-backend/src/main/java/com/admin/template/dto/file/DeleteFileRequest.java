package com.admin.template.dto.file;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "删除文件请求")
public record DeleteFileRequest(
        @Schema(title = "文件路径", description = "上传结果中的文件 URL 路径，仅允许删除公开文件目录内的文件。", example = "/uploads/1720000000000-ab12cd34ef56.pdf")
        @NotBlank String filePath) {
}
