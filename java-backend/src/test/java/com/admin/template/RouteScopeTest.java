package com.admin.template;

import com.admin.template.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AdminTemplateApplication.class)
@ActiveProfiles({"test", "unit-test"})
@AutoConfigureMockMvc
class RouteScopeTest {
    @TempDir
    static Path publicDirectory;

    @DynamicPropertySource
    static void uploadDirectory(DynamicPropertyRegistry registry) {
        registry.add("app.upload.directory", () -> publicDirectory.resolve("public").toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void preservesPlainTextRootGreeting() throws Exception {
        mockMvc.perform(get("/").header("X-Trace-Id", "root-contract"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/plain"))
                .andExpect(content().string("Hello World!"))
                .andExpect(header().string("X-Trace-Id", "root-contract"));
    }

    @Test
    void servesUploadedBytesAtTheReturnedPublicUrlWithoutAuthentication() throws Exception {
        byte[] bytes = "%PDF-1.4\npublic-file-contract".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "report.pdf", "application/pdf", bytes);
        String token = jwtService.createToken(7L, "uploader", List.of("operator"));

        String response = mockMvc.perform(multipart("/v1/files").file(file)
                        .param("path", "reports/2026")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString();
        String url = objectMapper.readTree(response).path("data").path("url").asText();

        assertThat(url).startsWith("/public/reports/2026/").endsWith(".pdf");
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/pdf"))
                .andExpect(content().bytes(bytes));
    }

    @Test
    void exposesSwaggerAndHeaderAuthenticationDocumentationWithoutJsonEnvelope() throws Exception {
        mockMvc.perform(get("/docs"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("swagger-ui")));
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.tags[*].name", hasItem("用户管理")))
                .andExpect(jsonPath("$.paths['/v1/users']").exists())
                .andExpect(jsonPath("$.paths['/v1/ai/evaluation-datasets']").exists())
                .andExpect(jsonPath("$.paths['/v1/ai/evaluation-datasets/{datasetId}/runs']").exists())
                .andExpect(jsonPath("$.components.securitySchemes").doesNotExist())
                .andExpect(jsonPath("$.paths['/v1/users'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/v1/auth/login'].post.security").doesNotExist())
                .andExpect(jsonPath("$.success").doesNotExist());
    }

    @Test
    void registersExactlyTheMigratedApplicationRoutes() {
        Set<String> routes = new TreeSet<>();
        handlerMapping.getHandlerMethods().forEach((mapping, handler) -> {
            if (handler.getBeanType().getPackageName().startsWith("com.admin.template.controller")) {
                for (String path : mapping.getPatternValues()) {
                    mapping.getMethodsCondition().getMethods().forEach(method -> routes.add(method + " " + path));
                }
            }
        });

        assertThat(routes).containsExactlyInAnyOrder(
                "GET /", "POST /v1/auth/login", "POST /v1/auth/logout", "GET /v1/me", "GET /v1/me/menus",
                "GET /v1/users", "POST /v1/users", "GET /v1/users/{id}", "PATCH /v1/users/{id}",
                "DELETE /v1/users/{id}", "GET /v1/users/export", "GET /v1/roles", "POST /v1/roles",
                "GET /v1/roles/{id}", "PATCH /v1/roles/{id}", "DELETE /v1/roles/{id}", "GET /v1/menus",
                "POST /v1/menus", "PATCH /v1/menus/{id}", "DELETE /v1/menus/{id}",
                "POST /v1/files", "POST /v1/files/batch", "DELETE /v1/files",
                "POST /v1/ai/knowledge-bases", "GET /v1/ai/knowledge-bases",
                "GET /v1/ai/knowledge-bases/{knowledgeBaseId}",
                "POST /v1/ai/knowledge-bases/{knowledgeBaseId}/documents",
                "POST /v1/ai/knowledge-bases/{knowledgeBaseId}/retrieval-preview",
                "GET /v1/ai/knowledge-bases/{knowledgeBaseId}/documents",
                "GET /v1/ai/knowledge-bases/{knowledgeBaseId}/documents/{documentId}",
                "GET /v1/ai/knowledge-bases/{knowledgeBaseId}/documents/{documentId}/content",
                "POST /v1/ai/knowledge-bases/{knowledgeBaseId}/documents/{documentId}/reindex",
                "POST /v1/ai/conversations", "GET /v1/ai/conversations",
                "GET /v1/ai/conversations/{conversationId}", "PATCH /v1/ai/conversations/{conversationId}",
                "DELETE /v1/ai/conversations/{conversationId}",
                "GET /v1/ai/conversations/{conversationId}/messages",
                "POST /v1/ai/conversations/{conversationId}/messages/stream",
                "GET /v1/ai/page-agent/workspace",
                "POST /v1/ai/page-agent/reset",
                "POST /v1/ai/page-agent/messages/stream",
                "POST /v1/ai/evaluation-datasets",
                "GET /v1/ai/evaluation-datasets",
                "PUT /v1/ai/evaluation-datasets/{datasetId}",
                "DELETE /v1/ai/evaluation-datasets/{datasetId}",
                "POST /v1/ai/evaluation-datasets/{datasetId}/cases",
                "GET /v1/ai/evaluation-datasets/{datasetId}/cases",
                "PUT /v1/ai/evaluation-datasets/{datasetId}/cases/{caseId}",
                "DELETE /v1/ai/evaluation-datasets/{datasetId}/cases/{caseId}",
                "POST /v1/ai/evaluation-datasets/{datasetId}/runs",
                "GET /v1/ai/evaluation-datasets/{datasetId}/runs",
                "GET /v1/ai/evaluation-datasets/{datasetId}/runs/{runId}");
    }
}
