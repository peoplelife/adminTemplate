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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;

@Service
public class AiRagService {
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String serviceToken;

    public AiRagService(
            @Value("${app.ai-rag.base-url}") String baseUrl,
            @Value("${app.ai-rag.service-token}") String serviceToken,
            @Value("${app.ai-rag.connect-timeout:3s}") Duration connectTimeout,
            @Value("${app.ai-rag.read-timeout:60s}") Duration readTimeout,
            ObjectMapper objectMapper) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(connectTimeout)
                        .version(HttpClient.Version.HTTP_1_1).build());
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.serviceToken = serviceToken;
        this.objectMapper = objectMapper;
    }

    public KnowledgeBaseResponse createKnowledgeBase(
            KnowledgeBaseCreateRequest request, AuthenticatedUser user, String traceId) {
        return jsonRequest(HttpMethod.POST, "/api/v1/rag/knowledge-bases", request,
                user, traceId, KnowledgeBaseResponse.class);
    }

    public KnowledgeBasePageResponse listKnowledgeBases(
            int page, int pageSize, AuthenticatedUser user, String traceId) {
        String uri = UriComponentsBuilder.fromPath("/api/v1/rag/knowledge-bases")
                .queryParam("page", page)
                .queryParam("page_size", pageSize)
                .build().encode().toUriString();
        return jsonRequest(HttpMethod.GET, uri, null, user, traceId, KnowledgeBasePageResponse.class);
    }

    public KnowledgeBaseResponse getKnowledgeBase(UUID id, AuthenticatedUser user, String traceId) {
        return jsonRequest(HttpMethod.GET, "/api/v1/rag/knowledge-bases/" + id,
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
            JsonNode response = restClient.post()
                    .uri("/api/v1/rag/knowledge-bases/{id}/documents", knowledgeBaseId)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .headers(headers -> addIdentityHeaders(headers, user, traceId))
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);
            return readResponse(response, DocumentResponse.class);
        } catch (IOException | RestClientException exception) {
            throw aiServiceFailure(exception);
        }
    }

    public DocumentPageResponse listDocuments(
            UUID knowledgeBaseId, int page, int pageSize, AuthenticatedUser user, String traceId) {
        String uri = UriComponentsBuilder.fromPath("/api/v1/rag/knowledge-bases/{id}/documents")
                .queryParam("page", page)
                .queryParam("page_size", pageSize)
                .buildAndExpand(knowledgeBaseId).encode().toUriString();
        return jsonRequest(HttpMethod.GET, uri, null, user, traceId, DocumentPageResponse.class);
    }

    public DocumentResponse getDocument(
            UUID knowledgeBaseId, UUID documentId, AuthenticatedUser user, String traceId) {
        return jsonRequest(HttpMethod.GET, documentPath(knowledgeBaseId, documentId),
                null, user, traceId, DocumentResponse.class);
    }

    public DocumentUploadContent getDocumentContent(
            UUID knowledgeBaseId, UUID documentId, AuthenticatedUser user, String traceId) {
        try {
            ResponseEntity<byte[]> response = restClient.get()
                    .uri(documentPath(knowledgeBaseId, documentId) + "/content")
                    .headers(headers -> addIdentityHeaders(headers, user, traceId))
                    .retrieve()
                    .toEntity(byte[].class);
            String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
            String filename = disposition == null ? "document" : ContentDisposition.parse(disposition).getFilename();
            filename = filename == null ? "document" : filename.replaceAll("[\\r\\n\\\"\\\\]", "_");
            MediaType contentType = response.getHeaders().getContentType();
            return new DocumentUploadContent(
                    filename,
                    contentType == null ? MediaType.APPLICATION_OCTET_STREAM : contentType,
                    response.getBody() == null ? new byte[0] : response.getBody());
        } catch (RestClientException exception) {
            throw aiServiceFailure(exception);
        }
    }

    public DocumentResponse reindexDocument(
            UUID knowledgeBaseId, UUID documentId, AuthenticatedUser user, String traceId) {
        return jsonRequest(HttpMethod.POST, documentPath(knowledgeBaseId, documentId) + "/reindex",
                null, user, traceId, DocumentResponse.class);
    }

    public RetrievalPreviewResponse previewRetrieval(
            UUID knowledgeBaseId,
            RetrievalPreviewRequest request,
            AuthenticatedUser user,
            String traceId) {
        String uri = "/api/v1/rag/knowledge-bases/" + knowledgeBaseId + "/retrieval-preview";
        return jsonRequest(HttpMethod.POST, uri, request, user, traceId, RetrievalPreviewResponse.class);
    }

    public ConversationResponse createConversation(
            ConversationCreateRequest request, AuthenticatedUser user, String traceId) {
        return jsonRequest(HttpMethod.POST, "/api/v1/rag/conversations", request,
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
        return jsonRequest(HttpMethod.GET, uri, null, user, traceId, ConversationPageResponse.class);
    }

    public ConversationResponse getConversation(
            UUID conversationId, AuthenticatedUser user, String traceId) {
        return jsonRequest(HttpMethod.GET, conversationPath(conversationId), null,
                user, traceId, ConversationResponse.class);
    }

    public ConversationResponse updateConversation(
            UUID conversationId, ConversationUpdateRequest request,
            AuthenticatedUser user, String traceId) {
        return jsonRequest(HttpMethod.PATCH, conversationPath(conversationId), request,
                user, traceId, ConversationResponse.class);
    }

    public void deleteConversation(UUID conversationId, AuthenticatedUser user, String traceId) {
        deleteResource(conversationPath(conversationId), user, traceId);
    }

    void deleteResource(String path, AuthenticatedUser user, String traceId) {
        try {
            ResponseEntity<String> response = restClient.delete()
                    .uri(path)
                    .headers(headers -> addIdentityHeaders(headers, user, traceId))
                    .retrieve().toEntity(String.class);
            if (response.getBody() != null && !response.getBody().isBlank()) {
                readResponse(objectMapper.readTree(response.getBody()), Object.class);
            }
            if (response.getStatusCode().value() != 204) throw aiServiceFailure(null);
        } catch (RestClientResponseException exception) {
            throw upstreamFailure(exception);
        } catch (RestClientException | IOException exception) {
            throw aiServiceFailure(exception);
        }
    }

    public MessagePageResponse listMessages(
            UUID conversationId, Long beforeSeq, int limit,
            AuthenticatedUser user, String traceId) {
        String uri = UriComponentsBuilder.fromPath(conversationPath(conversationId) + "/messages")
                .queryParam("limit", limit)
                .queryParamIfPresent("before_seq", java.util.Optional.ofNullable(beforeSeq))
                .build().encode().toUriString();
        return jsonRequest(HttpMethod.GET, uri, null, user, traceId, MessagePageResponse.class);
    }

    public StreamingResponseBody streamConversationMessage(
            UUID conversationId, MessageRequest request, AuthenticatedUser user,
            String traceId) {
        try {
            return restClient.post()
                    .uri(conversationPath(conversationId) + "/messages/stream")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .headers(headers -> addIdentityHeaders(headers, user, traceId))
                    .body(request)
                    .exchange((clientRequest, clientResponse) -> {
                        MediaType contentType = clientResponse.getHeaders().getContentType();
                        if (!clientResponse.getStatusCode().is2xxSuccessful()
                                || contentType == null
                                || !MediaType.TEXT_EVENT_STREAM.isCompatibleWith(contentType)) {
                            try (clientResponse) {
                                if (clientResponse.getStatusCode().is2xxSuccessful() && contentType != null
                                        && MediaType.APPLICATION_JSON.isCompatibleWith(contentType)) {
                                    byte[] errorBytes = clientResponse.getBody().readNBytes(65537);
                                    if (errorBytes.length <= 65536) {
                                        readResponse(objectMapper.readTree(errorBytes), Object.class);
                                    }
                                }
                                throw aiServiceFailure(null);
                            }
                        }
                        return (StreamingResponseBody) outputStream -> {
                            try (clientResponse; InputStream inputStream = clientResponse.getBody()) {
                                if (inputStream == null) {
                                    writeStreamError(outputStream, "AI 会话流为空", SystemError.AI_SERVICE_ERROR.code(), traceId);
                                    return;
                                }
                                byte[] buffer = new byte[8192];
                                int read;
                                while ((read = inputStream.read(buffer)) != -1) {
                                    outputStream.write(buffer, 0, read);
                                    outputStream.flush();
                                }
                            } catch (IOException exception) {
                                writeStreamError(outputStream, "AI 会话流中断", SystemError.AI_SERVICE_ERROR.code(), traceId);
                            }
                        };
                    }, false);
        } catch (RestClientException exception) {
            throw aiServiceFailure(exception);
        }
    }

    <T> T jsonRequest(
            HttpMethod method,
            String uri,
            Object body,
            AuthenticatedUser user,
            String traceId,
            Class<T> responseType) {
        try {
            var request = restClient.method(method).uri(uri)
                    .headers(headers -> addIdentityHeaders(headers, user, traceId));
            JsonNode response = body == null
                    ? request.retrieve().body(JsonNode.class)
                    : request.contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);
            return readResponse(response, responseType);
        } catch (RestClientResponseException exception) {
            throw upstreamFailure(exception);
        } catch (RestClientException exception) {
            throw aiServiceFailure(exception);
        }
    }

    private AppException upstreamFailure(RestClientResponseException exception) {
        try {
            JsonNode error = objectMapper.readTree(exception.getResponseBodyAsByteArray());
            if (error.path("code").asInt(-1) == SystemError.RAG_EMBEDDING_ERROR.code()) {
                return new AppException(SystemError.RAG_EMBEDDING_ERROR, null, exception);
            }
        } catch (IOException ignored) {
            return aiServiceFailure(exception);
        }
        return aiServiceFailure(exception);
    }

    private <T> T readResponse(JsonNode response, Class<T> responseType) {
        if (response == null || !response.isObject()) {
            throw aiServiceFailure(null);
        }
        int code = response.path("code").asInt(-1);
        if (code != 0) {
            BusinessError error = businessError(code);
            JsonNode detailsNode = response.has("details") ? response.get("details") : response.get("data");
            Object details = detailsNode == null || detailsNode.isNull() ? null : objectMapper.convertValue(detailsNode, Object.class);
            String message = response.path("message").asText(error.message());
            throw new BizException(error, message, details);
        }
        JsonNode data = response.get("data");
        if (data == null || !data.isObject()) {
            throw aiServiceFailure(null);
        }
        try {
            return objectMapper.treeToValue(data, responseType);
        } catch (JsonProcessingException exception) {
            throw aiServiceFailure(exception);
        }
    }

    private BusinessError businessError(int code) {
        for (BusinessError error : BusinessError.values()) {
            if (error.code() == code) return error;
        }
        throw aiServiceFailure(null);
    }

    private void addIdentityHeaders(HttpHeaders headers, AuthenticatedUser user, String traceId) {
        headers.set("X-Service-Token", serviceToken);
        headers.set("X-User-Id", Long.toString(user.id()));
        headers.set("X-Trace-Id", traceId);
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

    private void writeStreamError(OutputStream outputStream, String message, int code, String traceId) throws IOException {
        String data = objectMapper.writeValueAsString(java.util.Map.of(
                "code", code, "message", message, "traceId", traceId, "details", java.util.Map.of()));
        outputStream.write(("event: error\ndata: " + data + "\n\n").getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    private AppException aiServiceFailure(Throwable cause) {
        return new AppException(SystemError.AI_SERVICE_ERROR, null, cause);
    }

    public record DocumentUploadContent(String filename, MediaType contentType, byte[] bytes) {
    }
}
