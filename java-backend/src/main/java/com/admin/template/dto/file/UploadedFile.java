package com.admin.template.dto.file;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "上传文件信息")
public record UploadedFile(
        @Schema(title = "访问路径", description = "可公开访问的文件相对 URL。", example = "/uploads/1720000000000-ab12cd34ef56.pdf")
        String url,
        @Schema(title = "存储文件名", description = "服务端生成的文件名。", example = "1720000000000-ab12cd34ef56.pdf")
        String filename,
        @Schema(title = "原始文件名", description = "上传时的文件名。", example = "报告.pdf")
        String originalName,
        @Schema(title = "文件大小", description = "文件字节数。", example = "1024", minimum = "0")
        long size,
        @Schema(title = "文件 MIME 类型", description = "上传文件的媒体类型。", example = "application/pdf")
        String mimetype) {
}
