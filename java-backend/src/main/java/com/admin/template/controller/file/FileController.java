package com.admin.template.controller.file;

import com.admin.template.dto.file.DeleteFileRequest;
import com.admin.template.dto.file.UploadedFile;
import com.admin.template.service.file.FileService;
import com.admin.template.common.exception.BizException;
import com.admin.template.common.exception.BusinessError;
import com.admin.template.common.response.Result;
import com.admin.template.common.web.TraceIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Validated
@RestController
@RequestMapping("/v1/files")
@Tag(name = "文件管理", description = "上传和删除公开文件目录中的文件")
public class FileController {
    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "上传单个文件", description = "上传一个文件，大小不超过 10 MiB；支持 JPEG、PNG、GIF、WebP、PDF、Word 和 Excel。",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "multipart/form-data",
                            schema = @Schema(type = "object", requiredProperties = {"file"}),
                            schemaProperties = @SchemaProperty(name = "file",
                                    schema = @Schema(type = "string", format = "binary", description = "要上传的单个文件")))))
    public Result<UploadedFile> upload(@Parameter(hidden = true) @RequestPart(value = "file", required = false) List<MultipartFile> files,
                               @Parameter(description = "公开文件根目录下的相对目录；省略时为 uploads。", example = "reports/2026")
                               @RequestParam(required = false) String path,
                               @Parameter(hidden = true) @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        if (files != null && files.size() > 1) {
            throw new BizException(BusinessError.VALIDATION_ERROR, "文件数量超过限制（最多10个）", null);
        }
        return Result.ok(fileService.upload(files == null ? List.of() : files, path).getFirst(), "文件上传成功", traceId);
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "批量上传文件", description = "上传 1～10 个文件，每个不超过 10 MiB；支持 JPEG、PNG、GIF、WebP、PDF、Word 和 Excel。",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "multipart/form-data",
                            schema = @Schema(type = "object", requiredProperties = {"files"}),
                            schemaProperties = @SchemaProperty(name = "files",
                                    array = @ArraySchema(minItems = 1, maxItems = 10,
                                            schema = @Schema(type = "string", format = "binary", description = "要上传的文件"))))))
    public Result<List<UploadedFile>> uploadBatch(@Parameter(hidden = true) @RequestPart(value = "files", required = false) List<MultipartFile> files,
                                         @Parameter(description = "公开文件根目录下的相对目录；省略时为 uploads。", example = "reports/2026")
                                         @RequestParam(required = false) String path,
                                         @Parameter(hidden = true) @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(fileService.upload(files, path), "文件上传成功", traceId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除文件", description = "删除上传结果 URL 对应的文件。成功返回 204，无响应体。")
    public void delete(@Valid @RequestBody DeleteFileRequest request) {
        fileService.deleteFile(request.filePath());
    }
}
