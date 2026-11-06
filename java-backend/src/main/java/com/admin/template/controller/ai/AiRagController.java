package com.admin.template.controller.ai;

import com.admin.template.common.response.Result;
import com.admin.template.common.web.TraceIdFilter;
import com.admin.template.dto.ai.rag.RagDtos.DocumentPageResponse;
import com.admin.template.dto.ai.rag.RagDtos.DocumentResponse;
import com.admin.template.dto.ai.rag.RagDtos.KnowledgeBaseCreateRequest;
import com.admin.template.dto.ai.rag.RagDtos.KnowledgeBasePageResponse;
import com.admin.template.dto.ai.rag.RagDtos.KnowledgeBaseResponse;
import com.admin.template.dto.ai.rag.RagDtos.RetrievalPreviewRequest;
import com.admin.template.dto.ai.rag.RagDtos.RetrievalPreviewResponse;
import com.admin.template.dto.ai.rag.RagDtos.ConversationCreateRequest;
import com.admin.template.dto.ai.rag.RagDtos.ConversationPageResponse;
import com.admin.template.dto.ai.rag.RagDtos.ConversationResponse;
import com.admin.template.dto.ai.rag.RagDtos.ConversationUpdateRequest;
import com.admin.template.dto.ai.rag.RagDtos.MessagePageResponse;
import com.admin.template.dto.ai.rag.RagDtos.MessageRequest;
import com.admin.template.security.AuthenticatedUser;
import com.admin.template.service.ai.AiRagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/v1/ai")
@Tag(name = "AI 知识库", description = "私有知识库、文档上传和索引任务")
public class AiRagController {
    private final AiRagService service;

    public AiRagController(AiRagService service) {
        this.service = service;
    }

    @PostMapping("/knowledge-bases")
    @Operation(summary = "创建知识库", description = "创建当前登录用户拥有的私有知识库。")
    @ApiResponse(responseCode = "201", description = "创建成功",
            content = @Content(schema = @Schema(implementation = Result.class)))
    public ResponseEntity<Result<KnowledgeBaseResponse>> createKnowledgeBase(
            @Valid @RequestBody KnowledgeBaseCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Result.ok(service.createKnowledgeBase(request, user, traceId), traceId));
    }

    @GetMapping("/knowledge-bases")
    @Operation(summary = "查询知识库列表", description = "按当前登录用户分页查询私有知识库。")
    public Result<KnowledgeBasePageResponse> listKnowledgeBases(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(service.listKnowledgeBases(page, pageSize, user, traceId), traceId);
    }

    @GetMapping("/knowledge-bases/{knowledgeBaseId}")
    @Operation(summary = "查询知识库详情")
    public Result<KnowledgeBaseResponse> getKnowledgeBase(
            @PathVariable UUID knowledgeBaseId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(service.getKnowledgeBase(knowledgeBaseId, user, traceId), traceId);
    }

    @PostMapping(value = "/knowledge-bases/{knowledgeBaseId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传知识库文档", description = "上传 PDF、DOCX、TXT 或 Markdown 文件；返回前完成解析、切块和向量索引。")
    public Result<DocumentResponse> uploadDocument(
            @PathVariable UUID knowledgeBaseId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(service.uploadDocument(knowledgeBaseId, file, user, traceId), traceId);
    }

    @GetMapping("/knowledge-bases/{knowledgeBaseId}/documents")
    @Operation(summary = "查询知识库文档列表")
    public Result<DocumentPageResponse> listDocuments(
            @PathVariable UUID knowledgeBaseId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(service.listDocuments(knowledgeBaseId, page, pageSize, user, traceId), traceId);
    }

    @GetMapping("/knowledge-bases/{knowledgeBaseId}/documents/{documentId}")
    @Operation(summary = "查询文档详情")
    public Result<DocumentResponse> getDocument(
            @PathVariable UUID knowledgeBaseId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(service.getDocument(knowledgeBaseId, documentId, user, traceId), traceId);
    }

    @GetMapping("/knowledge-bases/{knowledgeBaseId}/documents/{documentId}/content")
    @Operation(summary = "下载文档原文", description = "鉴权后以附件形式下载私有文档，不返回 Result 包络。")
    public ResponseEntity<ByteArrayResource> getDocumentContent(
            @PathVariable UUID knowledgeBaseId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        AiRagService.DocumentUploadContent content = service.getDocumentContent(
                knowledgeBaseId, documentId, user, traceId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(content.contentType());
        headers.setContentDisposition(ContentDisposition.attachment().filename(content.filename()).build());
        headers.setContentLength(content.bytes().length);
        return ResponseEntity.ok().headers(headers).body(new ByteArrayResource(content.bytes()));
    }

    @PostMapping("/knowledge-bases/{knowledgeBaseId}/documents/{documentId}/reindex")
    @Operation(summary = "重新建立文档索引", description = "同步重新解析、切块并替换文档向量索引。")
    public Result<DocumentResponse> reindexDocument(
            @PathVariable UUID knowledgeBaseId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(service.reindexDocument(knowledgeBaseId, documentId, user, traceId), traceId);
    }

    @PostMapping("/knowledge-bases/{knowledgeBaseId}/retrieval-preview")
    @Operation(summary = "预览知识库检索", description = "仅检索知识库当前生效索引，不调用生成模型。")
    @ApiResponse(responseCode = "200", description = "检索成功",
            content = @Content(schema = @Schema(implementation = Result.class)))
    public Result<RetrievalPreviewResponse> previewRetrieval(
            @PathVariable UUID knowledgeBaseId,
            @Valid @RequestBody RetrievalPreviewRequest request,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(service.previewRetrieval(knowledgeBaseId, request, user, traceId), traceId);
    }

    @PostMapping("/conversations")
    @Operation(summary = "创建 AI 会话", description = "创建普通会话，或绑定当前用户拥有且已有可用文档的知识库会话。")
    public ResponseEntity<Result<ConversationResponse>> createConversation(
            @Valid @RequestBody ConversationCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Result.ok(service.createConversation(request, user, traceId), traceId));
    }

    @GetMapping("/conversations")
    @Operation(summary = "查询 AI 会话列表")
    public Result<ConversationPageResponse> listConversations(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(defaultValue = "all") String mode,
            @RequestParam(required = false) UUID knowledgeBaseId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(service.listConversations(page, pageSize, mode, knowledgeBaseId, user, traceId), traceId);
    }

    @GetMapping("/conversations/{conversationId}")
    @Operation(summary = "查询 AI 会话详情")
    public Result<ConversationResponse> getConversation(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(service.getConversation(conversationId, user, traceId), traceId);
    }

    @PatchMapping("/conversations/{conversationId}")
    @Operation(summary = "更新 AI 会话")
    public Result<ConversationResponse> updateConversation(
            @PathVariable UUID conversationId,
            @Valid @RequestBody ConversationUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(service.updateConversation(conversationId, request, user, traceId), traceId);
    }

    @DeleteMapping("/conversations/{conversationId}")
    @Operation(summary = "删除 AI 会话")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        service.deleteConversation(conversationId, user, traceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "查询 AI 会话消息历史")
    public Result<MessagePageResponse> listMessages(
            @PathVariable UUID conversationId,
            @RequestParam(required = false) Long beforeSeq,
            @RequestParam(defaultValue = "100") @Min(1) @Max(100) int limit,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(service.listMessages(conversationId, beforeSeq, limit, user, traceId), traceId);
    }

    @PostMapping(value = "/conversations/{conversationId}/messages/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "发送 AI 会话消息", description = "通过 SSE 返回 start、sources、message、done 或 error 事件。")
    @ApiResponse(responseCode = "200", description = "SSE 流式响应；开流前业务失败返回统一 JSON",
            content = {
                    @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE, schema = @Schema(type = "string")),
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Result.class))
            })
    public ResponseEntity<StreamingResponseBody> streamConversationMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody MessageRequest request,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        StreamingResponseBody body = service.streamConversationMessage(
                conversationId, request, user, traceId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_EVENT_STREAM);
        headers.setCacheControl(org.springframework.http.CacheControl.noCache());
        headers.set("X-Accel-Buffering", "no");
        return ResponseEntity.ok().headers(headers).body(body);
    }
}
