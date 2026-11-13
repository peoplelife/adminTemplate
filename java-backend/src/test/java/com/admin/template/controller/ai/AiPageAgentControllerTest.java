package com.admin.template.controller.ai;

import com.admin.template.AdminTemplateApplication;
import com.admin.template.entity.UserEntity;
import com.admin.template.mapper.UserMapper;
import com.admin.template.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AdminTemplateApplication.class)
@ActiveProfiles({"test", "unit-test"})
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class AiPageAgentControllerTest {
    private static final String ROOT = "/v1/ai/page-agent";
    private static final String SPEC = """
            {"pageKey":"employees","title":"Employees","fields":[{"key":"name","label":"Name","kind":"input","required":true,"searchable":true,"options":[]}],"actions":["create","edit","delete","detail"],"formColumns":2}
            """.strip();
    private static final String PREVIEW = """
            {"columns":[{"key":"name","label":"Name","type":"input","options":[]}],"filters":[{"key":"name","label":"Name","type":"input","options":[]}],"form":[{"key":"name","label":"Name","type":"input","required":true,"options":[]}],"actions":["create","edit","delete","detail"],"formColumns":2,"rows":[{"id":"1","name":"Alice","age":30,"active":true,"note":null}],"total":1}
            """.strip();
    private static final String WORKSPACE = "{\"code\":0,\"data\":{\"messages\":[{\"role\":\"user\",\"content\":\"员工管理\",\"reasoningContent\":\"\"},{\"role\":\"assistant\",\"content\":\"已生成\",\"reasoningContent\":\"先确认字段。\"}],\"pageSpec\":" + SPEC + ",\"preview\":" + PREVIEW + "}}";
    private static final AtomicReference<String> lastPath = new AtomicReference<>();
    private static final AtomicReference<String> lastMethod = new AtomicReference<>();
    private static final AtomicReference<String> lastUserId = new AtomicReference<>();
    private static final AtomicReference<String> lastTraceId = new AtomicReference<>();
    private static final AtomicReference<String> lastServiceToken = new AtomicReference<>();
    private static final AtomicReference<String> lastAuthorization = new AtomicReference<>();
    private static final AtomicReference<String> lastBody = new AtomicReference<>();
    private static final AtomicReference<String> responseBody = new AtomicReference<>();
    private static final AtomicReference<String> responseType = new AtomicReference<>();
    private static final AtomicInteger responseStatus = new AtomicInteger();
    private static HttpServer pythonServer;

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private UserMapper authenticationUsers;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws IOException {
        pythonServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        pythonServer.createContext("/api/v1/ai/page-agent", AiPageAgentControllerTest::handleStub);
        pythonServer.start();
        registry.add("app.ai-rag.base-url", () -> "http://127.0.0.1:" + pythonServer.getAddress().getPort());
        registry.add("app.ai-rag.service-token", () -> "internal-page-agent-token");
    }

    @BeforeEach
    void setUp() {
        when(authenticationUsers.selectById(anyLong())).thenAnswer(invocation -> {
            var user = new UserEntity();
            user.id = invocation.getArgument(0);
            user.status = true;
            return user;
        });
        lastPath.set(null);
        responseStatus.set(200);
        responseBody.set(WORKSPACE);
        responseType.set("application/json");
    }

    @AfterAll
    static void stopStub() {
        if (pythonServer != null) pythonServer.stop(0);
    }

    @Test
    void restoresTypedWorkspaceUsingTrustedIdentityAndTrace() throws Exception {
        mockMvc.perform(auth(get(ROOT + "/workspace")).header("X-User-Id", "999")
                        .header("X-Service-Token", "forged").header("X-Trace-Id", "page-workspace-trace"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.messages[0].role").value("user"))
                .andExpect(jsonPath("$.data.messages[0].content").value("员工管理"))
                .andExpect(jsonPath("$.data.messages[1].content").value("已生成"))
                .andExpect(jsonPath("$.data.messages[1].reasoningContent").value("先确认字段。"))
                .andExpect(jsonPath("$.data.pageSpec.pageKey").value("employees"))
                .andExpect(jsonPath("$.data.pageSpec.fields[0].searchable").value(true))
                .andExpect(jsonPath("$.data.preview.form[0].required").value(true))
                .andExpect(jsonPath("$.data.preview.rows[0].age").value(30))
                .andExpect(jsonPath("$.data.preview.rows[0].active").value(true))
                .andExpect(jsonPath("$.data.preview.total").value(1))
                .andExpect(jsonPath("$.traceId").value("page-workspace-trace"));
        assertThat(lastPath.get()).isEqualTo("/api/v1/ai/page-agent/workspace");
        assertThat(lastMethod.get()).isEqualTo("GET");
        assertThat(lastUserId.get()).isEqualTo("42");
        assertThat(lastServiceToken.get()).isEqualTo("internal-page-agent-token");
        assertThat(lastTraceId.get()).isEqualTo("page-workspace-trace");
        assertThat(lastAuthorization.get()).isNull();
    }

    @Test
    void resetsWorkspaceAsJsonWithNullablePage() throws Exception {
        responseBody.set("{\"code\":0,\"data\":{\"messages\":[],\"pageSpec\":null,\"preview\":null}}");
        mockMvc.perform(auth(post(ROOT + "/reset")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.messages").isEmpty())
                .andExpect(jsonPath("$.data.pageSpec").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.preview").value(org.hamcrest.Matchers.nullValue()));
        assertThat(lastMethod.get()).isEqualTo("POST");
        assertThat(lastPath.get()).isEqualTo("/api/v1/ai/page-agent/reset");
    }

    @Test
    void forwardsTemplateRetrievalConfigurationAndTypedSampleData() throws Exception {
        responseBody.set("""
                {"code":0,"data":{"messages":[],"pageSpec":{
                "pageKey":"employees","title":"Employees","fields":[{
                "key":"name","label":"Name","kind":"input","required":true,"searchable":true,"options":[],
                "column":{"width":180,"minWidth":130,"align":"left","fixed":null,"sortable":true,"showOverflowTooltip":true},
                "attrs":{"placeholder":"Enter name","clearable":true,"disabled":false,"maxlength":80,"min":null,"max":null,"step":null,"precision":null},"defaultValue":null}],
                "actions":["create","edit","detail"],"formColumns":2,
                "table":{"border":true,"stripe":true,"size":"small","showHeader":true,"height":480},
                "pagination":{"enabled":true,"pageSize":20,"pageSizes":[10,20,50],"background":true},
                "form":{"labelPosition":"left","labelWidth":100,"size":"default","disabled":false},
                "sampleData":[{"name":"Alice","score":98,"ratio":0.5,"note":null}]},"preview":{
                "columns":[{"key":"name","label":"Name","type":"input","options":[],
                "column":{"width":180,"minWidth":130,"align":"left","fixed":null,"sortable":true,"showOverflowTooltip":true},"attrs":null,"defaultValue":null}],
                "filters":[],"form":[{"key":"name","label":"Name","type":"input","required":true,"options":[],"column":null,
                "attrs":{"placeholder":"Enter name","clearable":true,"disabled":false,"maxlength":80,"min":null,"max":null,"step":null,"precision":null},"defaultValue":null}],
                "actions":["create","edit","detail"],"formColumns":2,
                "rows":[{"id":"1","name":"Alice","score":98,"ratio":0.5,"note":null}],"total":1}}}
                """);
        mockMvc.perform(auth(get(ROOT + "/workspace")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageSpec.table.border").value(true))
                .andExpect(jsonPath("$.data.pageSpec.table.height").value(480))
                .andExpect(jsonPath("$.data.pageSpec.pagination.pageSizes[2]").value(50))
                .andExpect(jsonPath("$.data.pageSpec.form.labelPosition").value("left"))
                .andExpect(jsonPath("$.data.pageSpec.fields[0].column.sortable").value(true))
                .andExpect(jsonPath("$.data.pageSpec.fields[0].attrs.maxlength").value(80))
                .andExpect(jsonPath("$.data.pageSpec.fields[0].defaultValue").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.pageSpec.sampleData[0].score").value(98))
                .andExpect(jsonPath("$.data.pageSpec.sampleData[0].ratio").value(0.5))
                .andExpect(jsonPath("$.data.preview.columns[0].column.width").value(180))
                .andExpect(jsonPath("$.data.preview.form[0].attrs.placeholder").value("Enter name"));
    }

    @Test
    void requiresAuthenticationForAllPageAgentRoutes() throws Exception {
        for (var request : List.of(get(ROOT + "/workspace"), post(ROOT + "/reset"),
                post(ROOT + "/messages/stream").contentType(APPLICATION_JSON).content("{\"content\":\"Hello\"}"))) {
            mockMvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(110005));
        }
        assertThat(lastPath.get()).isNull();
    }

    @Test
    void preservesStringNumberAndNullDefaultValues() throws Exception {
        for (String value : List.of("\"Alice\"", "17.5", "null")) {
            var workspace = (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper.readTree(WORKSPACE);
            ((com.fasterxml.jackson.databind.node.ObjectNode) workspace.at("/data/pageSpec/fields/0"))
                    .set("defaultValue", objectMapper.readTree(value));
            responseBody.set(objectMapper.writeValueAsString(workspace));
            String json = mockMvc.perform(auth(get(ROOT + "/workspace"))).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(objectMapper.readTree(json).at("/data/pageSpec/fields/0/defaultValue"))
                    .isEqualTo(objectMapper.readTree(value));
        }
    }

    @Test
    void rejectsMissingBlankAndOversizedContentBeforeCallingPython() throws Exception {
        for (String body : List.of("{}", "{\"content\":null}", "{\"content\":\"   \"}",
                objectMapper.writeValueAsString(java.util.Map.of("content", "x".repeat(4001))))) {
            mockMvc.perform(auth(post(ROOT + "/messages/stream")).contentType(APPLICATION_JSON).content(body))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(100001));
        }
        assertThat(lastPath.get()).isNull();
    }

    @Test
    void forwardsAllSseEventsAndHeartbeatWithoutEnvelopeOrModel() throws Exception {
        String stream = ": heartbeat\n\nevent: status\ndata: {\"stage\":\"analyzing\",\"message\":\"分析需求\",\"traceId\":\"page-stream\"}\n\n"
                + "event: reasoning\ndata: {\"content\":\"先确认\"}\n\nevent: reasoning\ndata: {\"content\":\"字段。\"}\n\n"
                + "event: message\ndata: {\"content\":\"已生成\"}\n\nevent: clarification\ndata: {\"question\":\"还需哪些字段？\"}\n\n"
                + "event: page_spec\ndata: " + SPEC + "\n\nevent: preview\ndata: " + PREVIEW + "\n\n"
                + "event: done\ndata: {\"status\":\"completed\",\"pageChanged\":true}\n\n"
                + "event: error\ndata: {\"code\":160001,\"message\":\"AI 服务暂时不可用\",\"traceId\":\"page-stream\"}\n\n";
        responseType.set("text/event-stream");
        responseBody.set(stream);
        var result = mockMvc.perform(auth(post(ROOT + "/messages/stream")).header("X-Trace-Id", "page-stream")
                        .contentType(APPLICATION_JSON).content("{\"content\":\"员工管理\",\"model\":\"untrusted\"}"))
                .andExpect(status().isOk()).andExpect(request().asyncStarted())
                .andExpect(content().contentTypeCompatibleWith("text/event-stream"))
                .andExpect(header().string("Cache-Control", "no-cache"))
                .andExpect(header().string("X-Accel-Buffering", "no")).andReturn();
        result.getAsyncResult(10000);
        assertThat(mockMvc.perform(asyncDispatch(result)).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8)).isEqualTo(stream);
        assertThat(objectMapper.readTree(lastBody.get())).isEqualTo(objectMapper.readTree("{\"content\":\"员工管理\"}"));
        assertThat(lastUserId.get()).isEqualTo("42");
        assertThat(lastTraceId.get()).isEqualTo("page-stream");
    }

    @Test
    void handlesJsonBusinessFailureBeforeStartingSse() throws Exception {
        responseBody.set("{\"code\":100001,\"message\":\"Invalid request\",\"data\":null}");
        mockMvc.perform(auth(post(ROOT + "/messages/stream")).contentType(APPLICATION_JSON).content("{\"content\":\"Hi\"}"))
                .andExpect(status().isOk()).andExpect(request().asyncNotStarted())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(100001)).andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void mapsUpstreamHttpFailuresWithoutLeakingResponseContent(CapturedOutput output) throws Exception {
        responseStatus.set(503);
        responseBody.set("secret-provider-key");
        for (var request : List.of(get(ROOT + "/workspace"), post(ROOT + "/reset"),
                post(ROOT + "/messages/stream").contentType(APPLICATION_JSON).content("{\"content\":\"Hi\"}"))) {
            mockMvc.perform(auth(request).header("X-Trace-Id", "upstream-page-error"))
                    .andExpect(status().isBadGateway()).andExpect(request().asyncNotStarted())
                    .andExpect(jsonPath("$.code").value(160001))
                    .andExpect(jsonPath("$.traceId").value("upstream-page-error"))
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret-provider-key"))));
        }
        assertThat(output.getAll()).doesNotContain("secret-provider-key");
    }

    @Test
    void rejectsMalformedJsonAndNonSseSuccessBeforeOpeningStream() throws Exception {
        for (String body : List.of("not-json", "{}", "{\"code\":0,\"data\":{}}")) {
            responseBody.set(body);
            mockMvc.perform(auth(post(ROOT + "/messages/stream")).contentType(APPLICATION_JSON).content("{\"content\":\"Hi\"}"))
                    .andExpect(status().isBadGateway()).andExpect(request().asyncNotStarted())
                    .andExpect(jsonPath("$.code").value(160001));
        }
    }

    @Test
    void rejectsInvalidUpstreamContentTypeBeforeOpeningStream() throws Exception {
        responseType.set("invalid-content-type");
        mockMvc.perform(auth(post(ROOT + "/messages/stream")).contentType(APPLICATION_JSON).content("{\"content\":\"Hi\"}"))
                .andExpect(status().isBadGateway()).andExpect(request().asyncNotStarted())
                .andExpect(jsonPath("$.code").value(160001));
    }

    @Test
    void documentsCompleteTypedWorkspaceAndSseAndExportsOpenApi() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode api = objectMapper.readTree(json);
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target/page-agent-openapi.json"), json, StandardCharsets.UTF_8);
        assertThat(api.at("/paths/~1v1~1ai~1page-agent~1workspace/get/responses/200/content/application~1json/schema/$ref").asText())
                .contains("PageAgentWorkspace");
        assertThat(api.at("/components/schemas/PageAgentWorkspace/properties").has("pageSpec")).isTrue();
        assertThat(api.at("/components/schemas/PageAgentPageSpec/properties/fields/items/$ref").asText()).contains("PageAgentField");
        assertThat(api.at("/components/schemas/PageAgentPreview/properties").has("rows")).isTrue();
        assertThat(api.at("/components/schemas/PageAgentMessageRequest/properties/content/maxLength").asInt()).isEqualTo(4000);
        assertThat(api.at("/components/schemas/PageAgentWorkspaceMessage/properties/content/maxLength").asInt()).isEqualTo(8000);
        assertThat(api.at("/components/schemas/PageAgentWorkspaceMessage/properties/reasoningContent/type").asText()).isEqualTo("string");
        assertThat(api.at("/components/schemas/PageAgentMessageRequest/properties").has("model")).isFalse();
        assertThat(api.at("/components/schemas/PageAgentPageSpec/properties/table/$ref").asText()).contains("PageAgentTableConfig");
        assertThat(api.at("/components/schemas/PageAgentPageSpec/properties/pagination/$ref").asText()).contains("PageAgentPaginationConfig");
        assertThat(api.at("/components/schemas/PageAgentPageSpec/properties/form/$ref").asText()).contains("PageAgentFormConfig");
        assertThat(api.at("/components/schemas/PageAgentPageSpec/properties/sampleData/type").toString()).contains("array");
        assertThat(api.at("/components/schemas/PageAgentField/properties/column/$ref").asText()).contains("PageAgentColumnConfig");
        assertThat(api.at("/components/schemas/PageAgentField/properties/attrs/$ref").asText()).contains("PageAgentControlAttrs");
        assertThat(api.at("/components/schemas/PageAgentField/properties/defaultValue/type").toString()).contains("string", "number", "null");
        assertThat(api.at("/components/schemas/PageAgentField/properties/defaultValue").has("$ref")).isFalse();
        assertThat(api.at("/components/schemas/PageAgentTableConfig/properties/size/enum").toString()).contains("small", "default", "large");
        assertThat(api.at("/components/schemas/PageAgentTableConfig/properties/height/minimum").asInt()).isEqualTo(160);
        assertThat(api.at("/components/schemas/PageAgentPreviewColumn/properties/column/$ref").asText()).contains("PageAgentColumnConfig");
        assertThat(api.at("/components/schemas/PageAgentPreviewFormField/properties/attrs/$ref").asText()).contains("PageAgentControlAttrs");
        assertThat(api.at("/components/schemas/PageAgentPageSpec/properties/actions/items/enum").toString())
                .contains("create", "edit", "delete", "detail");
        assertThat(api.at("/components/schemas/PageAgentOption/properties/color/type").toString()).contains("null");
        JsonNode stream = api.at("/paths/~1v1~1ai~1page-agent~1messages~1stream/post");
        assertThat(stream.path("description").asText()).contains("status", "reasoning", "clarification", "page_spec", "preview", "done", "error");
        assertThat(stream.path("parameters").isMissingNode() || stream.path("parameters").isEmpty()).isTrue();
        assertThat(stream.at("/responses/200/content/text~1event-stream").isMissingNode()).isFalse();
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + jwtService.createToken(42L, "alice", List.of("operator")));
    }

    private static void handleStub(HttpExchange exchange) throws IOException {
        lastPath.set(exchange.getRequestURI().getPath());
        lastMethod.set(exchange.getRequestMethod());
        lastUserId.set(exchange.getRequestHeaders().getFirst("X-User-Id"));
        lastTraceId.set(exchange.getRequestHeaders().getFirst("X-Trace-Id"));
        lastServiceToken.set(exchange.getRequestHeaders().getFirst("X-Service-Token"));
        lastAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
        lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        byte[] body = responseBody.get().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", responseType.get());
        exchange.sendResponseHeaders(responseStatus.get(), body.length);
        try (exchange; var output = exchange.getResponseBody()) {
            output.write(body);
        }
    }
}
