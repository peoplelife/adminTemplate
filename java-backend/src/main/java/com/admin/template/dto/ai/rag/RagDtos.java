package com.admin.template.dto.ai.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class RagDtos {
    private RagDtos() {
    }

    @Schema(name = "RagKnowledgeBaseCreateRequest", description = "创建私有知识库")
    public record KnowledgeBaseCreateRequest(
            @Schema(title = "知识库名称", example = "员工手册")
            @NotBlank @Size(max = 100) String name,
            @Schema(title = "知识库简介", example = "公司制度与员工政策", nullable = true)
            @Size(max = 1000) String description) {
    }

    @Schema(name = "RagKnowledgeBase", description = "知识库概要")
    public record KnowledgeBaseResponse(
            @Schema(title = "知识库 ID", example = "28927c14-4dcf-49db-9a91-d81629cb2392") UUID id,
            @Schema(title = "知识库名称", example = "员工手册") String name,
            @Schema(title = "知识库简介", nullable = true) String description,
            @Schema(title = "状态", allowableValues = {"active", "disabled", "deleted"}) String status,
            @Schema(title = "Embedding 模型", example = "BAAI/bge-m3") String embeddingModel,
            @Schema(title = "向量维度", example = "1024") int embeddingDimension,
            @Schema(title = "切块字符数", example = "600") int chunkSize,
            @Schema(title = "切块重叠字符数", example = "100") int chunkOverlap,
            @Schema(title = "文档数", example = "3") int documentCount,
            @Schema(title = "已就绪文档数", example = "2") int readyDocumentCount,
            @Schema(title = "创建时间") Instant createdAt,
            @Schema(title = "更新时间") Instant updatedAt) {
    }

    @Schema(name = "RagKnowledgeBasePage", description = "知识库页码分页")
    public record KnowledgeBasePageResponse(
            @Schema(title = "知识库列表") List<KnowledgeBaseResponse> items,
            @Schema(title = "总条数") long total,
            @Schema(title = "当前页码") int page,
            @Schema(title = "每页数量") int pageSize) {
    }

    @Schema(name = "RagDocument", description = "知识库文档")
    public record DocumentResponse(
            @Schema(title = "文档 ID") UUID id,
            @Schema(title = "知识库 ID") UUID knowledgeBaseId,
            @Schema(title = "原始文件名") String fileName,
            @Schema(title = "检测后的 MIME 类型") String mimeType,
            @Schema(title = "文件大小（字节）") long fileSize,
            @Schema(title = "文档状态") String status,
            @Schema(title = "生效索引版本") int activeGeneration,
            @Schema(title = "最新索引版本") int latestGeneration,
            @Schema(title = "生效片段数") int chunkCount,
            @Schema(title = "PDF 页数", nullable = true) Integer pageCount,
            @Schema(title = "创建时间") Instant createdAt,
            @Schema(title = "更新时间") Instant updatedAt) {
    }

    @Schema(name = "RagDocumentPage", description = "文档页码分页")
    public record DocumentPageResponse(
            @Schema(title = "文档列表") List<DocumentResponse> items,
            @Schema(title = "总条数") long total,
            @Schema(title = "当前页码") int page,
            @Schema(title = "每页数量") int pageSize) {
    }

    @Schema(name = "RagRetrievalPreviewRequest", description = "知识库检索预览请求")
    public record RetrievalPreviewRequest(
            @Schema(title = "检索问题", example = "员工年假有多少天？")
            @NotBlank @Size(max = 4000) String query,
            @Schema(title = "返回片段数", example = "5", defaultValue = "5", minimum = "1", maximum = "20")
            @Min(1) @Max(20) Integer topK) {
        public RetrievalPreviewRequest {
            topK = topK == null ? 5 : topK;
        }
    }

    @Schema(name = "RagRetrievalPreviewItem", description = "检索命中的文档片段")
    public record RetrievalPreviewItem(
            @Schema(title = "片段 ID") UUID chunkId,
            @Schema(title = "文档 ID") UUID documentId,
            @Schema(title = "原始文件名") String fileName,
            @Schema(title = "片段内容") String content,
            @Schema(title = "起始页码", nullable = true) Integer pageFrom,
            @Schema(title = "结束页码", nullable = true) Integer pageTo,
            @Schema(title = "章节标题", nullable = true) String sectionTitle,
            @Schema(title = "余弦相似度", example = "0.82") double similarity) {
    }

    @Schema(name = "RagRetrievalPreviewResponse", description = "知识库检索预览结果")
    public record RetrievalPreviewResponse(
            @Schema(title = "知识库 ID") UUID knowledgeBaseId,
            @Schema(title = "检索问题") String query,
            @Schema(title = "按余弦相似度排序的命中片段") List<RetrievalPreviewItem> items,
            @Schema(title = "检索耗时（毫秒）", example = "42.5") double elapsedMs) {
    }

    @Schema(name = "RagConversationCreateRequest", description = "创建 AI 会话")
    public record ConversationCreateRequest(
            @Schema(title = "会话标题", example = "员工政策", nullable = true)
            @Size(max = 200) String title,
            @Schema(title = "绑定的知识库 ID；为空表示普通对话", nullable = true)
            UUID knowledgeBaseId,
            @Schema(title = "生成模型", example = "deepseek-chat", nullable = true)
            @Size(max = 100) String model) {
    }

    @Schema(name = "RagConversationUpdateRequest", description = "更新 AI 会话")
    public record ConversationUpdateRequest(
            @Schema(title = "会话标题", example = "员工政策（已整理）", nullable = true)
            @Size(min = 1, max = 200) String title,
            @Schema(title = "会话状态", allowableValues = {"active", "archived"}, nullable = true)
            String status) {
    }

    @Schema(name = "RagConversation", description = "AI 会话")
    public record ConversationResponse(
            @Schema(title = "会话 ID") UUID conversationId,
            @Schema(title = "会话标题") String title,
            @Schema(title = "绑定的知识库 ID", nullable = true) UUID knowledgeBaseId,
            @Schema(title = "生成模型") String model,
            @Schema(title = "会话状态", allowableValues = {"active", "archived"}) String status,
            @Schema(title = "创建时间") Instant createdAt,
            @Schema(title = "更新时间") Instant updatedAt) {
    }

    @Schema(name = "RagConversationPage", description = "AI 会话页码分页")
    public record ConversationPageResponse(
            @Schema(title = "会话列表") List<ConversationResponse> items,
            @Schema(title = "总条数") long total,
            @Schema(title = "当前页码") int page,
            @Schema(title = "每页数量") int pageSize) {
    }

    @Schema(name = "RagMessageRequest", description = "发送 AI 消息")
    public record MessageRequest(
            @Schema(title = "客户端幂等请求 ID", example = "38927c14-4dcf-49db-9a91-d81629cb2393")
            @NotNull UUID clientRequestId,
            @Schema(title = "消息内容", example = "员工年假有多少天？")
            @NotBlank @Size(max = 4000) String content) {
    }

    @Schema(name = "RagMessageSource", description = "AI 回复引用来源")
    public record MessageSourceResponse(
            @Schema(title = "引用序号") int citationIndex,
            @Schema(title = "文档 ID") UUID documentId,
            @Schema(title = "文档名称") String documentName,
            @Schema(title = "起始页码", nullable = true) Integer pageFrom,
            @Schema(title = "结束页码", nullable = true) Integer pageTo,
            @Schema(title = "章节标题", nullable = true) String sectionTitle,
            @Schema(title = "来源摘录") String excerpt,
            @Schema(title = "来源是否仍可访问") boolean available) {
    }

    @Schema(name = "RagMessage", description = "AI 会话消息")
    public record MessageResponse(
            @Schema(title = "消息 ID") UUID id,
            @Schema(title = "消息序号") long seq,
            @Schema(title = "消息角色", allowableValues = {"user", "assistant"}) String role,
            @Schema(title = "消息内容") String content,
            @Schema(title = "消息状态") String status,
            @Schema(title = "引用来源") List<MessageSourceResponse> sources,
            @Schema(title = "创建时间") Instant createdAt) {
    }

    @Schema(name = "RagMessagePage", description = "AI 消息游标分页")
    public record MessagePageResponse(
            @Schema(title = "消息列表") List<MessageResponse> items,
            @Schema(title = "下一页游标", nullable = true) String nextCursor) {
    }
}
