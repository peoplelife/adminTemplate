package com.admin.template.service.ai;

import com.admin.template.common.exception.AppException;
import com.admin.template.common.exception.BizException;
import com.admin.template.common.exception.BusinessError;
import com.admin.template.common.exception.SystemError;
import com.admin.template.security.AuthenticatedUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Shared, authenticated transport for the Python AI service. */
@Service
public class AiServiceClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String serviceToken;

    public AiServiceClient(
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

    public <T> T jsonRequest(HttpMethod method, String uri, Object body,
            AuthenticatedUser user, String traceId, Class<T> responseType) {
        return jsonRequest(method, uri, body, MediaType.APPLICATION_JSON, user, traceId, responseType);
    }

    <T> T multipartRequest(String uri, Object body,
            AuthenticatedUser user, String traceId, Class<T> responseType) {
        return jsonRequest(HttpMethod.POST, uri, body, MediaType.MULTIPART_FORM_DATA, user, traceId, responseType);
    }

    private <T> T jsonRequest(HttpMethod method, String uri, Object body, MediaType contentType,
            AuthenticatedUser user, String traceId, Class<T> responseType) {
        try {
            var request = restClient.method(method).uri(uri)
                    .headers(headers -> addIdentityHeaders(headers, user, traceId));
            JsonNode response = body == null
                    ? request.retrieve().body(JsonNode.class)
                    : request.contentType(contentType).body(body).retrieve().body(JsonNode.class);
            return readResponse(response, responseType);
        } catch (RestClientResponseException exception) {
            throw upstreamFailure(exception);
        } catch (RestClientException exception) {
            throw aiServiceFailure(exception);
        }
    }

    ResponseEntity<byte[]> downloadResource(String uri, AuthenticatedUser user, String traceId) {
        try {
            return restClient.get().uri(uri).headers(headers -> addIdentityHeaders(headers, user, traceId))
                    .retrieve().toEntity(byte[].class);
        } catch (RestClientResponseException exception) {
            throw upstreamFailure(exception);
        } catch (RestClientException exception) {
            throw aiServiceFailure(exception);
        }
    }

    public void deleteResource(String path, AuthenticatedUser user, String traceId) {
        try {
            ResponseEntity<String> response = restClient.delete().uri(path)
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

    public StreamingResponseBody streamRequest(String uri, Object body,
            AuthenticatedUser user, String traceId) {
        try {
            return restClient.post().uri(uri).contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .headers(headers -> addIdentityHeaders(headers, user, traceId))
                    .body(body)
                    .exchange((clientRequest, clientResponse) -> {
                        MediaType contentType;
                        try {
                            contentType = clientResponse.getHeaders().getContentType();
                        } catch (IllegalArgumentException exception) {
                            clientResponse.close();
                            throw aiServiceFailure(exception);
                        }
                        if (!clientResponse.getStatusCode().is2xxSuccessful()
                                || contentType == null
                                || !MediaType.TEXT_EVENT_STREAM.isCompatibleWith(contentType)) {
                            try (clientResponse) {
                                InputStream input = clientResponse.getBody();
                                byte[] bytes = input == null ? new byte[0] : input.readNBytes(65537);
                                if (bytes.length <= 65536 && bytes.length > 0) {
                                    JsonNode node = objectMapper.readTree(bytes);
                                    if (node != null && node.isObject() && node.has("code")) {
                                        readResponse(node, Object.class);
                                    }
                                }
                                throw aiServiceFailure(null);
                            }
                        }
                        return (StreamingResponseBody) outputStream -> {
                            try (clientResponse; InputStream inputStream = clientResponse.getBody()) {
                                if (inputStream == null) {
                                    writeStreamError(outputStream, "AI 会话流为空", traceId);
                                    return;
                                }
                                byte[] buffer = new byte[8192];
                                int read;
                                while ((read = inputStream.read(buffer)) != -1) {
                                    outputStream.write(buffer, 0, read);
                                    outputStream.flush();
                                }
                            } catch (IOException exception) {
                                writeStreamError(outputStream, "AI 会话流中断", traceId);
                            }
                        };
                    }, false);
        } catch (RestClientException exception) {
            throw aiServiceFailure(exception);
        }
    }

    private void addIdentityHeaders(HttpHeaders headers, AuthenticatedUser user, String traceId) {
        headers.set("X-Service-Token", serviceToken);
        headers.set("X-User-Id", Long.toString(user.id()));
        headers.set("X-Trace-Id", traceId);
    }

    private AppException upstreamFailure(RestClientResponseException exception) {
        try {
            JsonNode error = objectMapper.readTree(exception.getResponseBodyAsByteArray());
            if (error != null && error.path("code").asInt(-1) == SystemError.RAG_EMBEDDING_ERROR.code()) {
                return new AppException(SystemError.RAG_EMBEDDING_ERROR, null, safeCause(exception));
            }
        } catch (IOException ignored) {
            // Do not expose arbitrary upstream response content.
        }
        return aiServiceFailure(exception);
    }

    private <T> T readResponse(JsonNode response, Class<T> responseType) {
        if (response == null || !response.isObject()) throw aiServiceFailure(null);
        int code = response.path("code").asInt(-1);
        if (code != 0) {
            BusinessError error = businessError(code);
            JsonNode detailsNode = response.has("details") ? response.get("details") : response.get("data");
            Object details = detailsNode == null || detailsNode.isNull()
                    ? null : objectMapper.convertValue(detailsNode, Object.class);
            String message = response.path("message").asText(error.message());
            throw new BizException(error, message, details);
        }
        JsonNode data = response.get("data");
        if (data == null || !data.isObject()) throw aiServiceFailure(null);
        try {
            return objectMapper.treeToValue(data, responseType);
        } catch (JsonProcessingException exception) {
            throw aiServiceFailure(exception);
        }
    }

    private BusinessError businessError(int code) {
        for (BusinessError error : BusinessError.values()) if (error.code() == code) return error;
        throw aiServiceFailure(null);
    }

    private void writeStreamError(OutputStream outputStream, String message, String traceId) throws IOException {
        String data = objectMapper.writeValueAsString(java.util.Map.of(
                "code", SystemError.AI_SERVICE_ERROR.code(), "message", message, "traceId", traceId));
        outputStream.write(("event: error\ndata: " + data + "\n\n").getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    private AppException aiServiceFailure(Throwable cause) {
        return new AppException(SystemError.AI_SERVICE_ERROR, null, safeCause(cause));
    }

    private Throwable safeCause(Throwable cause) {
        // HTTP/parser exception messages may contain the upstream body or credentials.
        return cause == null ? null : new IOException("AI transport failed: " + cause.getClass().getSimpleName());
    }
}
