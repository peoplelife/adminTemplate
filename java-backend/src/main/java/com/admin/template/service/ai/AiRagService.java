package com.admin.template.service.ai;

import com.admin.template.common.exception.AppException;
import com.admin.template.common.exception.BizException;
import com.admin.template.common.exception.BusinessError;
import com.admin.template.common.exception.SystemError;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.UUID;

@Service
public class AiRagService {
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private final AiServiceClient client;

    public AiRagService(AiServiceClient client) {
        this.client = client;
    }

    public KnowledgeBaseResponse createKnowledgeBase(
            KnowledgeBaseCreateRequest request, AuthenticatedUser user, String traceId) {
        return client.jsonRequest(HttpMethod.POST, "/api/v1/rag/knowledge-bases", request,
                user, traceId, KnowledgeBaseResponse.class);
    }

    public KnowledgeBasePageResponse listKnowledgeBases(
            int page, int pageSize, AuthenticatedUser user, String traceId) {
        String uri = UriComponentsBuilder.fromPath("/api/v1/rag/knowledge-bases")
                .queryParam("page", page)
                .queryParam("page_size", pageSize)
                .build().encode().toUriString();
        return client.jsonRequest(HttpMethod.GET, uri, null, user, traceId, KnowledgeBasePageResponse.class);
    }

    public KnowledgeBaseResponse getKnowledgeBase(UUID id, AuthenticatedUser user, String traceId) {
        return client.jsonRequest(HttpMethod.GET, "/api/v1/rag/knowledge-bases/" + id,
                null, user, traceId, KnowledgeBaseResponse.class);
    }

    public DocumentResponse uploadDocument(
            UUID knowledgeBaseId, MultipartFile file, AuthenticatedUser user, String traceId) {
        if (file == null || file.isEmpty()) {
            throw new BizException(BusinessError.VALIDATION_ERROR, "请选择要上传的文件", null);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(BusinessError.VALIDATION_ERROR, "文件大小超过限制（最大20 MiB）", null);
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new BizException(BusinessError.VALIDATION_ERROR, "文件名不能为空", null);
        }

        try {
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return fileName.replace("\\", "_").replace("/", "_");
                }
            };
            HttpHeaders partHeaders = new HttpHeaders();
            partHeaders.setContentType(parseContentType(file.getContentType()));
            MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
            form.add("file", new HttpEntity<>(resource, partHeaders));
            return client.multipartRequest("/api/v1/rag/knowledge-bases/" + knowledgeBaseId + "/documents",
                    form, user, traceId, DocumentResponse.class);
        } catch (IOException exception) {
            throw aiServiceFailure(exception);
        }
    }

    public DocumentPageResponse listDocuments(
            UUID knowledgeBaseId, int page, int pageSize, AuthenticatedUser user, String traceId) {
        String uri = UriComponentsBuilder.fromPath("/api/v1/rag/knowledge-bases/{id}/documents")
                .queryParam("page", page)
                .queryParam("page_size", pageSize)
                .buildAndExpand(knowledgeBaseId).encode().toUriString();
        return client.jsonRequest(HttpMethod.GET, uri, null, user, traceId, DocumentPageResponse.class);
    }

    public DocumentResponse getDocument(
            UUID knowledgeBaseId, UUID documentId, AuthenticatedUser user, String traceId) {
        return client.jsonRequest(HttpMethod.GET, documentPath(knowledgeBaseId, documentId),
                null, user, traceId, DocumentResponse.class);
    }

    public DocumentUploadContent getDocumentContent(
            UUID knowledgeBaseId, UUID documentId, AuthenticatedUser user, String traceId) {
        ResponseEntity<byte[]> response = client.downloadResource(
                documentPath(knowledgeBaseId, documentId) + "/content", user, traceId);
        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        String filename = disposition == null ? "document" : ContentDisposition.parse(disposition).getFilename();
        filename = filename == null ? "document" : filename.replaceAll("[\\r\\n\\\"\\\\]", "_");
        MediaType contentType = response.getHeaders().getContentType();
        return new DocumentUploadContent(
                filename,
                contentType == null ? MediaType.APPLICATION_OCTET_STREAM : contentType,
                response.getBody() == null ? new byte[0] : response.getBody());
    }

    public DocumentResponse reindexDocument(
            UUID knowledgeBaseId, UUID documentId, AuthenticatedUser user, String traceId) {
        return client.jsonRequest(HttpMethod.POST, documentPath(knowledgeBaseId, documentId) + "/reindex",
                null, user, traceId, DocumentResponse.class);
    }

    public RetrievalPreviewResponse previewRetrieval(
            UUID knowledgeBaseId,
            RetrievalPreviewRequest request,
            AuthenticatedUser user,
            String traceId) {
        String uri = "/api/v1/rag/knowledge-bases/" + knowledgeBaseId + "/retrieval-preview";
        return client.jsonRequest(HttpMethod.POST, uri, request, user, traceId, RetrievalPreviewResponse.class);
    }

    public ConversationResponse createConversation(
            ConversationCreateRequest request, AuthenticatedUser user, String traceId) {
        return client.jsonRequest(HttpMethod.POST, "/api/v1/rag/conversations", request,
                user, traceId, ConversationResponse.class);
    }

    public ConversationPageResponse listConversations(
            int page, int pageSize, String mode, UUID knowledgeBaseId,
            AuthenticatedUser user, String traceId) {
        String uri = UriComponentsBuilder.fromPath("/api/v1/rag/conversations")
                .queryParam("page", page)
                .queryParam("page_size", pageSize)
                .queryParamIfPresent("mode", java.util.Optional.ofNullable(mode))
                .queryParamIfPresent("knowledge_base_id", java.util.Optional.ofNullable(knowledgeBaseId))
                .build().encode().toUriString();
        return client.jsonRequest(HttpMethod.GET, uri, null, user, traceId, ConversationPageResponse.class);
    }

    public ConversationResponse getConversation(
            UUID conversationId, AuthenticatedUser user, String traceId) {
        return client.jsonRequest(HttpMethod.GET, conversationPath(conversationId), null,
                user, traceId, ConversationResponse.class);
    }

    public ConversationResponse updateConversation(
            UUID conversationId, ConversationUpdateRequest request,
            AuthenticatedUser user, String traceId) {
        return client.jsonRequest(HttpMethod.PATCH, conversationPath(conversationId), request,
                user, traceId, ConversationResponse.class);
    }

    public void deleteConversation(UUID conversationId, AuthenticatedUser user, String traceId) {
        client.deleteResource(conversationPath(conversationId), user, traceId);
    }

    public MessagePageResponse listMessages(
            UUID conversationId, Long beforeSeq, int limit,
            AuthenticatedUser user, String traceId) {
        String uri = UriComponentsBuilder.fromPath(conversationPath(conversationId) + "/messages")
                .queryParam("limit", limit)
                .queryParamIfPresent("before_seq", java.util.Optional.ofNullable(beforeSeq))
                .build().encode().toUriString();
        return client.jsonRequest(HttpMethod.GET, uri, null, user, traceId, MessagePageResponse.class);
    }

    public StreamingResponseBody streamConversationMessage(
            UUID conversationId, MessageRequest request, AuthenticatedUser user,
            String traceId) {
        return client.streamRequest(conversationPath(conversationId) + "/messages/stream", request, user, traceId);
    }

    private MediaType parseContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) return MediaType.APPLICATION_OCTET_STREAM;
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private String documentPath(UUID knowledgeBaseId, UUID documentId) {
        return "/api/v1/rag/knowledge-bases/" + knowledgeBaseId + "/documents/" + documentId;
    }

    private String conversationPath(UUID conversationId) {
        return "/api/v1/rag/conversations/" + conversationId;
    }

    private AppException aiServiceFailure(Throwable cause) {
        return new AppException(SystemError.AI_SERVICE_ERROR, null, cause);
    }

    public record DocumentUploadContent(String filename, MediaType contentType, byte[] bytes) {
    }
}
