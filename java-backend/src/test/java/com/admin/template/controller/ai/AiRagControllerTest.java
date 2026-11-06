package com.admin.template.controller.ai;

import com.admin.template.AdminTemplateApplication;
import com.admin.template.security.JwtService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest(classes = AdminTemplateApplication.class)
@ActiveProfiles({"test", "unit-test"})
@AutoConfigureMockMvc
class AiRagControllerTest {
    private static final AtomicReference<String> lastMethod = new AtomicReference<>();
    private static final AtomicReference<String> lastPath = new AtomicReference<>();
    private static final AtomicReference<String> lastQuery = new AtomicReference<>();
    private static final AtomicReference<String> lastServiceToken = new AtomicReference<>();
    private static final AtomicReference<String> lastUserId = new AtomicReference<>();
    private static final AtomicReference<String> lastTraceId = new AtomicReference<>();
    private static final AtomicReference<String> lastBody = new AtomicReference<>();
    private static final AtomicReference<String> lastUpgrade = new AtomicReference<>();
    private static final AtomicInteger responseCode = new AtomicInteger(200);
    private static final AtomicInteger retrievalBusinessCode = new AtomicInteger(0);
    private static HttpServer pythonServer;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;
    @org.springframework.boot.test.mock.mockito.MockBean
    private com.admin.template.mapper.UserMapper authenticationUsers;

    @org.junit.jupiter.api.BeforeEach
    void setUpAuthenticationAccount() {
        org.mockito.Mockito.when(authenticationUsers.selectById(org.mockito.ArgumentMatchers.anyLong()))
                .thenAnswer(invocation -> {
                    var user = new com.admin.template.entity.UserEntity();
                    user.id = invocation.getArgument(0);
                    user.status = true;
                    return user;
                });
    }

    @DynamicPropertySource
    static void ragProperties(DynamicPropertyRegistry registry) {
        startPythonStub();
        registry.add("app.ai-rag.base-url", () -> "http://127.0.0.1:" + pythonServer.getAddress().getPort());
        registry.add("app.ai-rag.service-token", () -> "internal-rag-token");
    }

    @BeforeEach
    void resetStub() {
        lastMethod.set(null);
        lastPath.set(null);
        lastQuery.set(null);
        lastServiceToken.set(null);
        lastUserId.set(null);
        lastTraceId.set(null);
        lastBody.set(null);
        lastUpgrade.set(null);
        responseCode.set(200);
        retrievalBusinessCode.set(0);
    }

    @AfterAll
    static void stopPythonStub() {
        if (pythonServer != null) pythonServer.stop(0);
    }

    @Test
    void evaluationUsesTrustedIdentityTypedPagesAndTraceId() throws Exception {
        String token = jwtService.createToken(42L, "alice", List.of("operator"));
        mockMvc.perform(get("/v1/ai/evaluation-datasets").param("page", "2").param("pageSize", "10")
                        .header("Authorization", "Bearer " + token).header("X-User-Id", "forged")
                        .header("X-Trace-Id", "evaluation-trace"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(11)).andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.traceId").value("evaluation-trace"));
        assertThat(lastUserId.get()).isEqualTo("42");
        assertThat(lastQuery.get()).contains("page=2", "page_size=10");
        assertThat(lastTraceId.get()).isEqualTo("evaluation-trace");

        mockMvc.perform(post("/v1/ai/evaluation-datasets").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON).content("{\"name\":\"Policy evaluation\",\"knowledgeBaseId\":\"28927c14-4dcf-49db-9a91-d81629cb2392\",\"description\":\"\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.name").value("Policy evaluation"));
        mockMvc.perform(post("/v1/ai/evaluation-datasets/28927c14-4dcf-49db-9a91-d81629cb2392/runs")
                        .header("Authorization", "Bearer " + token).contentType(APPLICATION_JSON).content("{\"topK\":5,\"generateAnswers\":true}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.metrics.recallAtK").value(1.0))
                .andExpect(jsonPath("$.data.results[0].retrievedItems[0].generation").value(3))
                .andExpect(jsonPath("$.data.results[0].generatedAnswer").value("annual leave"))
                .andExpect(jsonPath("$.data.results[0].generationDetails.judge.reason").value("supported"));
        assertThat(lastBody.get()).contains("\"generateAnswers\":true");
        mockMvc.perform(delete("/v1/ai/evaluation-datasets/28927c14-4dcf-49db-9a91-d81629cb2392")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
    }

    @Test
    void evaluationRejectsMissingIdentityAndInvalidPageBounds() throws Exception {
        mockMvc.perform(get("/v1/ai/evaluation-datasets")).andExpect(jsonPath("$.success").value(false));
        String token = jwtService.createToken(42L, "alice", List.of("operator"));
        lastPath.set(null);
        mockMvc.perform(get("/v1/ai/evaluation-datasets").param("page", "0")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.success").value(false));
        assertThat(lastPath.get()).isNull();
    }

    @Test
    void createsKnowledgeBaseWithTrustedHeadersAndTypedResponse() throws Exception {
        String token = jwtService.createToken(42L, "alice", List.of("operator"));

        mockMvc.perform(post("/v1/ai/knowledge-bases")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Trace-Id", "rag-request-1")
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Employee handbook\",\"description\":\"Policies\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Employee handbook"))
                .andExpect(jsonPath("$.data.embeddingModel").value("BAAI/bge-m3"));

        assertThat(lastMethod.get()).isEqualTo("POST");
        assertThat(lastPath.get()).isEqualTo("/api/v1/rag/knowledge-bases");
        assertThat(lastServiceToken.get()).isEqualTo("internal-rag-token");
        assertThat(lastUserId.get()).isEqualTo("42");
        assertThat(lastTraceId.get()).isEqualTo("rag-request-1");
        assertThat(lastUpgrade.get()).isNull();
    }

    @Test
    void uploadsMultipartDocumentAndReturnsReadyDocument() throws Exception {
        String token = jwtService.createToken(42L, "alice", List.of("operator"));
        MockMultipartFile file = new MockMultipartFile(
                "file", "policy.md", "text/markdown", "# Policy".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/v1/ai/knowledge-bases/28927c14-4dcf-49db-9a91-d81629cb2392/documents")
                        .file(file)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Trace-Id", "rag-upload-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("48927c14-4dcf-49db-9a91-d81629cb2394"))
                .andExpect(jsonPath("$.data.status").value("ready"))
                .andExpect(jsonPath("$.data.activeGeneration").value(1));

        assertThat(lastPath.get()).isEqualTo("/api/v1/rag/knowledge-bases/28927c14-4dcf-49db-9a91-d81629cb2392/documents");
        assertThat(lastUserId.get()).isEqualTo("42");
        assertThat(lastBody.get()).contains("# Policy");
    }

    @Test
    void reindexesDocumentSynchronously() throws Exception {
        String token = jwtService.createToken(42L, "alice", List.of("operator"));

        mockMvc.perform(post("/v1/ai/knowledge-bases/28927c14-4dcf-49db-9a91-d81629cb2392/documents/48927c14-4dcf-49db-9a91-d81629cb2394/reindex")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ready"))
                .andExpect(jsonPath("$.data.latestGeneration").value(1));

        assertThat(lastPath.get()).endsWith("/reindex");
    }

    @Test
    void mapsPythonBusinessCodeToTheJavaResultContract() throws Exception {
        responseCode.set(200);
        String token = jwtService.createToken(42L, "alice", List.of("operator"));

        mockMvc.perform(get("/v1/ai/knowledge-bases/28927c14-4dcf-49db-9a91-d81629cb2392")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(160101));
    }

    @Test
    void mapsPythonTransportFailureToBadGateway() throws Exception {
        responseCode.set(503);
        String token = jwtService.createToken(42L, "alice", List.of("operator"));

        mockMvc.perform(get("/v1/ai/knowledge-bases/28927c14-4dcf-49db-9a91-d81629cb2392")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value(160001));
    }

    @Test
    void previewsKnowledgeBaseRetrievalWithTrustedHeadersAndTypedMatches() throws Exception {
        String token = jwtService.createToken(42L, "alice", List.of("operator"));

        mockMvc.perform(post("/v1/ai/knowledge-bases/28927c14-4dcf-49db-9a91-d81629cb2392/retrieval-preview")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Trace-Id", "retrieval-preview-1")
                        .contentType(APPLICATION_JSON)
                        .content("{\"query\":\"annual leave\",\"topK\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.query").value("annual leave"))
                .andExpect(jsonPath("$.data.items[0].fileName").value("policy.md"))
                .andExpect(jsonPath("$.data.items[0].pageFrom").value(3))
                .andExpect(jsonPath("$.data.items[0].similarity").value(0.82))
                .andExpect(jsonPath("$.data.elapsedMs").value(42.5));

        assertThat(lastMethod.get()).isEqualTo("POST");
        assertThat(lastPath.get()).isEqualTo(
                "/api/v1/rag/knowledge-bases/28927c14-4dcf-49db-9a91-d81629cb2392/retrieval-preview");
        assertThat(lastServiceToken.get()).isEqualTo("internal-rag-token");
        assertThat(lastUserId.get()).isEqualTo("42");
        assertThat(lastTraceId.get()).isEqualTo("retrieval-preview-1");
        assertThat(lastBody.get()).contains("\"query\":\"annual leave\"").contains("\"topK\":3");
    }

    @Test
    void mapsRetrievalBusinessCodeToNumericResult() throws Exception {
        retrievalBusinessCode.set(160103);
        String token = jwtService.createToken(42L, "alice", List.of("operator"));

        mockMvc.perform(post("/v1/ai/knowledge-bases/28927c14-4dcf-49db-9a91-d81629cb2392/retrieval-preview")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"query\":\"annual leave\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(160103));
    }

    @Test
    void mapsEmbeddingProviderFailureToTheRag502Error() throws Exception {
        responseCode.set(502);
        String token = jwtService.createToken(42L, "alice", List.of("operator"));

        mockMvc.perform(post("/v1/ai/knowledge-bases/28927c14-4dcf-49db-9a91-d81629cb2392/retrieval-preview")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"query\":\"annual leave\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value(160501));
    }

    @Test
    void createsConversationWithTypedResponseAndTrustedHeaders() throws Exception {
        String token = jwtService.createToken(42L, "alice", List.of("operator"));

        mockMvc.perform(post("/v1/ai/conversations")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Trace-Id", "conversation-create-1")
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Policies\",\"model\":\"deepseek-chat\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Policies"))
                .andExpect(jsonPath("$.data.conversationId").value("28927c14-4dcf-49db-9a91-d81629cb2392"));

        assertThat(lastPath.get()).isEqualTo("/api/v1/rag/conversations");
        assertThat(lastServiceToken.get()).isEqualTo("internal-rag-token");
        assertThat(lastUserId.get()).isEqualTo("42");
        assertThat(lastTraceId.get()).isEqualTo("conversation-create-1");
        assertThat(lastBody.get()).contains("\"model\":\"deepseek-chat\"");
    }

    @Test
    void listsConversationAndMessageHistoryWithTypedDtos() throws Exception {
        String token = jwtService.createToken(42L, "alice", List.of("operator"));

        mockMvc.perform(get("/v1/ai/conversations")
                        .param("page", "2")
                        .param("pageSize", "10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].title").value("Policies"))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.total").value(11))
                .andExpect(jsonPath("$.data.pageSize").value(10));
        assertThat(lastQuery.get()).contains("page=2", "page_size=10").doesNotContain("cursor", "limit");

        mockMvc.perform(get("/v1/ai/conversations/28927c14-4dcf-49db-9a91-d81629cb2392/messages")
                        .param("limit", "20")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].role").value("assistant"))
                .andExpect(jsonPath("$.data.items[0].sources[0].documentName").value("policy.md"));

        assertThat(lastPath.get()).endsWith("/messages");
        assertThat(lastUserId.get()).isEqualTo("42");
    }

    @Test
    void updatesAndDeletesConversationUsingJsonAndEmptyDeleteResponse() throws Exception {
        String token = jwtService.createToken(42L, "alice", List.of("operator"));
        String path = "/v1/ai/conversations/28927c14-4dcf-49db-9a91-d81629cb2392";

        mockMvc.perform(patch(path)
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Renamed\",\"status\":\"archived\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Renamed"))
                .andExpect(jsonPath("$.data.status").value("archived"));

        mockMvc.perform(delete(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void mapsConversationBusinessCodeWithoutChangingHttpStatus() throws Exception {
        retrievalBusinessCode.set(160121);
        String token = jwtService.createToken(42L, "alice", List.of("operator"));

        mockMvc.perform(get("/v1/ai/conversations/28927c14-4dcf-49db-9a91-d81629cb2392")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(160121));
    }

    @Test
    void forwardsConversationSseEventsWithoutResultEnvelope() throws Exception {
        String token = jwtService.createToken(42L, "alice", List.of("operator"));

        var result = mockMvc.perform(post("/v1/ai/conversations/28927c14-4dcf-49db-9a91-d81629cb2392/messages/stream")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Trace-Id", "conversation-stream-1")
                        .contentType(APPLICATION_JSON)
                        .content("{\"clientRequestId\":\"38927c14-4dcf-49db-9a91-d81629cb2393\",\"content\":\"What is the leave policy?\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/event-stream"))
                .andReturn();

        result.getAsyncResult(10000);
        String stream = mockMvc.perform(asyncDispatch(result)).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(stream).contains("event: start", "event: sources", "event: message", "event: done")
                .doesNotContain("\"data\":{\"code\"");
        assertThat(lastPath.get()).endsWith("/messages/stream");
        assertThat(lastServiceToken.get()).isEqualTo("internal-rag-token");
        assertThat(lastUserId.get()).isEqualTo("42");
        assertThat(lastTraceId.get()).isEqualTo("conversation-stream-1");
        assertThat(stream).contains("\"traceId\":\"conversation-stream-1\"");
    }

    @Test
    void reportsUpstreamConversationPreflightBusinessErrorAsJson() throws Exception {
        retrievalBusinessCode.set(160123);
        String token = jwtService.createToken(42L, "alice", List.of("operator"));
        var result = mockMvc.perform(post("/v1/ai/conversations/28927c14-4dcf-49db-9a91-d81629cb2392/messages/stream")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"clientRequestId\":\"38927c14-4dcf-49db-9a91-d81629cb2393\",\"content\":\"Hello\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(160123))
                .andExpect(jsonPath("$.success").value(false))
                .andReturn();
        assertThat(result.getRequest().isAsyncStarted()).isFalse();
    }

    private static synchronized void startPythonStub() {
        if (pythonServer != null) return;
        try {
            pythonServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            pythonServer.createContext("/api/v1/rag", AiRagControllerTest::handleStub);
            pythonServer.start();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not start Python RAG stub", exception);
        }
    }

    private static void handleStub(HttpExchange exchange) throws IOException {
        lastMethod.set(exchange.getRequestMethod());
        lastUpgrade.set(exchange.getRequestHeaders().getFirst("Upgrade"));
        lastPath.set(exchange.getRequestURI().getPath());
        lastQuery.set(exchange.getRequestURI().getQuery());
        lastServiceToken.set(exchange.getRequestHeaders().getFirst("X-Service-Token"));
        lastUserId.set(exchange.getRequestHeaders().getFirst("X-User-Id"));
        lastTraceId.set(exchange.getRequestHeaders().getFirst("X-Trace-Id"));
        lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

        String body;
        int status = responseCode.get();
        String path = lastPath.get();
        if (path.startsWith("/api/v1/rag/evaluation-datasets") && "DELETE".equals(exchange.getRequestMethod())) {
            body = "";
            status = 204;
        } else if (path.startsWith("/api/v1/rag/evaluation-datasets") && path.endsWith("/runs")) {
            body = "{\"code\":0,\"data\":{\"id\":\"38927c14-4dcf-49db-9a91-d81629cb2393\",\"datasetId\":\"28927c14-4dcf-49db-9a91-d81629cb2392\",\"status\":\"completed\",\"topK\":5,\"totalCases\":1,\"metrics\":{\"hitAtK\":1,\"recallAtK\":1,\"mrr\":1,\"evaluatedCases\":1,\"failedCases\":0,\"unanswerableCases\":0},\"config\":{\"embedding_model\":\"synthetic\",\"scorer_version\":\"evidence_contains@v1\",\"documents\":[]},\"results\":[{\"status\":\"completed\",\"caseSnapshot\":{\"question\":\"leave?\",\"answerable\":true,\"expectedEvidence\":[]},\"retrievedItems\":[{\"generation\":3,\"content\":\"annual leave\"}],\"recallAtK\":1}]}}";
            body = body.replace("\"retrievedItems\":", "\"generatedAnswer\":\"annual leave\",\"generationStatus\":\"completed\",\"generationDetails\":{\"context\":\"annual leave\",\"judge\":{\"status\":\"completed\",\"reason\":\"supported\"}},\"retrievedItems\":");
        } else if (path.equals("/api/v1/rag/evaluation-datasets") && "POST".equals(exchange.getRequestMethod())) {
            body = "{\"code\":0,\"data\":{\"id\":\"28927c14-4dcf-49db-9a91-d81629cb2392\",\"knowledgeBaseId\":\"28927c14-4dcf-49db-9a91-d81629cb2392\",\"name\":\"Policy evaluation\",\"description\":\"\"}}";
        } else if (path.equals("/api/v1/rag/evaluation-datasets")) {
            body = "{\"code\":0,\"data\":{\"items\":[{\"id\":\"28927c14-4dcf-49db-9a91-d81629cb2392\",\"knowledgeBaseId\":\"28927c14-4dcf-49db-9a91-d81629cb2392\",\"name\":\"Policy evaluation\",\"description\":\"\"}],\"total\":11,\"page\":2,\"pageSize\":10}}";
        } else if (path.endsWith("/messages/stream") && retrievalBusinessCode.get() == 160123) {
            body = "{\"code\":160123,\"message\":\"Conversation already has a generating request\",\"data\":null}";
        } else if (path.endsWith("/messages/stream")) {
            body = "event: start\ndata: {\"traceId\":\"" + lastTraceId.get()
                    + "\",\"conversationId\":\"28927c14-4dcf-49db-9a91-d81629cb2392\"}\n\n"
                    + "event: sources\ndata: {\"items\":[]}\n\n"
                    + "event: message\ndata: {\"content\":\"Leave is 20 days.\"}\n\n"
                    + "event: done\ndata: {\"status\":\"completed\"}\n\n";
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
        } else if (path.endsWith("/messages")) {
            body = "{\"code\":0,\"message\":\"success\",\"data\":{\"items\":[{\"id\":\"38927c14-4dcf-49db-9a91-d81629cb2393\",\"seq\":2,\"role\":\"assistant\",\"content\":\"Leave is 20 days.\",\"status\":\"completed\",\"sources\":[{\"citationIndex\":1,\"documentId\":\"48927c14-4dcf-49db-9a91-d81629cb2394\",\"documentName\":\"policy.md\",\"excerpt\":\"Annual leave\",\"available\":true}],\"createdAt\":\"2026-10-29T03:00:00Z\"}],\"nextCursor\":null}}";
        } else if (path.equals("/api/v1/rag/conversations") && "POST".equals(exchange.getRequestMethod())) {
            body = "{\"code\":0,\"message\":\"success\",\"data\":{\"conversationId\":\"28927c14-4dcf-49db-9a91-d81629cb2392\",\"title\":\"Policies\",\"knowledgeBaseId\":null,\"model\":\"deepseek-chat\",\"status\":\"active\",\"createdAt\":\"2026-10-29T03:00:00Z\",\"updatedAt\":\"2026-10-29T03:00:00Z\"}}";
        } else if (path.equals("/api/v1/rag/conversations")) {
            body = "{\"code\":0,\"message\":\"success\",\"data\":{\"items\":[{\"conversationId\":\"28927c14-4dcf-49db-9a91-d81629cb2392\",\"title\":\"Policies\",\"knowledgeBaseId\":null,\"model\":\"deepseek-chat\",\"status\":\"active\",\"createdAt\":\"2026-10-29T03:00:00Z\",\"updatedAt\":\"2026-10-29T03:00:00Z\"}],\"total\":11,\"page\":2,\"pageSize\":10}}";
        } else if (retrievalBusinessCode.get() == 160121 && path.contains("/conversations/")) {
            body = "{\"code\":160121,\"message\":\"Conversation was not found\",\"data\":null}";
        } else if (path.contains("/conversations/") && "DELETE".equals(exchange.getRequestMethod())) {
            body = "";
            status = 204;
        } else if (path.contains("/conversations/") && "PATCH".equals(exchange.getRequestMethod())) {
            body = "{\"code\":0,\"message\":\"success\",\"data\":{\"conversationId\":\"28927c14-4dcf-49db-9a91-d81629cb2392\",\"title\":\"Renamed\",\"knowledgeBaseId\":null,\"model\":\"deepseek-chat\",\"status\":\"archived\",\"createdAt\":\"2026-10-29T03:00:00Z\",\"updatedAt\":\"2026-10-29T03:00:00Z\"}}";
        } else if (path.contains("/conversations/")) {
            body = "{\"code\":0,\"message\":\"success\",\"data\":{\"conversationId\":\"28927c14-4dcf-49db-9a91-d81629cb2392\",\"title\":\"Policies\",\"knowledgeBaseId\":null,\"model\":\"deepseek-chat\",\"status\":\"active\",\"createdAt\":\"2026-10-29T03:00:00Z\",\"updatedAt\":\"2026-10-29T03:00:00Z\"}}";
        } else if ("GET".equals(exchange.getRequestMethod())) {
            body = "{\"code\":160101,\"message\":\"Knowledge base was not found\",\"data\":null}";
        } else if (lastPath.get().endsWith("/retrieval-preview") && responseCode.get() == 502) {
            body = "{\"code\":160501,\"message\":\"Embedding service request failed\",\"data\":null}";
        } else if (lastPath.get().endsWith("/retrieval-preview") && retrievalBusinessCode.get() != 0) {
            body = "{\"code\":" + retrievalBusinessCode.get()
                    + ",\"message\":\"Knowledge base has no ready documents\",\"data\":null}";
        } else if (lastPath.get().endsWith("/retrieval-preview")) {
            body = "{\"code\":0,\"message\":\"success\",\"data\":{\"knowledgeBaseId\":\"28927c14-4dcf-49db-9a91-d81629cb2392\",\"query\":\"annual leave\",\"items\":[{\"chunkId\":\"58927c14-4dcf-49db-9a91-d81629cb2395\",\"documentId\":\"48927c14-4dcf-49db-9a91-d81629cb2394\",\"fileName\":\"policy.md\",\"content\":\"Employees may take annual leave.\",\"pageFrom\":3,\"pageTo\":3,\"sectionTitle\":\"Leave policy\",\"similarity\":0.82}],\"elapsedMs\":42.5}}";
        } else if (lastPath.get().endsWith("/documents") || lastPath.get().endsWith("/reindex")) {
            body = "{\"code\":0,\"message\":\"success\",\"data\":{\"id\":\"48927c14-4dcf-49db-9a91-d81629cb2394\",\"knowledgeBaseId\":\"28927c14-4dcf-49db-9a91-d81629cb2392\",\"fileName\":\"policy.md\",\"mimeType\":\"text/markdown\",\"fileSize\":8,\"status\":\"ready\",\"activeGeneration\":1,\"latestGeneration\":1,\"chunkCount\":1,\"pageCount\":null,\"createdAt\":\"2026-10-29T03:00:00Z\",\"updatedAt\":\"2026-10-29T03:00:00Z\"}}";
        } else {
            body = "{\"code\":0,\"message\":\"success\",\"data\":{\"id\":\"28927c14-4dcf-49db-9a91-d81629cb2392\",\"name\":\"Employee handbook\",\"description\":\"Policies\",\"status\":\"active\",\"embeddingModel\":\"BAAI/bge-m3\",\"embeddingDimension\":1024,\"chunkSize\":600,\"chunkOverlap\":100,\"documentCount\":0,\"readyDocumentCount\":0,\"createdAt\":\"2026-10-29T03:00:00Z\",\"updatedAt\":\"2026-10-29T03:00:00Z\"}}";
        }
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        if (exchange.getResponseHeaders().getFirst("Content-Type") == null) {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
        }
        exchange.sendResponseHeaders(status, response.length);
        if (response.length > 0) exchange.getResponseBody().write(response);
        exchange.close();
    }
}
