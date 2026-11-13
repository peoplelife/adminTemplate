package com.admin.template.controller.auth;

import com.admin.template.AdminTemplateApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AdminTemplateApplication.class, properties = "app.auth.github.enabled=false")
@ActiveProfiles("test")
@AutoConfigureMockMvc
class GitHubOAuthControllerTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    @Test
    void exposesOnlyTheFourExactPublicRoutesAndDisablesOAuthByDefault() throws Exception {
        mvc.perform(get("/v1/auth/github/config").header("X-Trace-Id", "github-config"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.traceId").value("github-config"));
        mvc.perform(post("/v1/auth/github/authorize"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(110008));
        mvc.perform(post("/v1/auth/github/complete").contentType("application/json").content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(100001));
        for (String path : new String[]{"/v1/me/github", "/v1/me/github/authorize", "/v1/me/github/complete",
                "/v1/auth/github/config-extra", "/v1/auth/github/callback/extra", "/v1/auth/github/unknown"}) {
            mvc.perform(get(path)).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(110005));
        }
    }

    @Test
    void exportsTheCompleteOpenApiWithResultSchemasAndNoBrowserTokenParameters() throws Exception {
        String document = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var api = json.readTree(document);
        assertThat(api.at("/paths/~1v1~1auth~1github~1authorize/post/operationId").asText())
                .isEqualTo("githubAuthorize");
        assertThat(api.at("/paths/~1v1~1auth~1github~1complete/post/description").asText())
                .contains("首次授权", "普通用户");
        assertThat(api.at("/paths/~1v1~1me~1github/delete/description").asText()).contains("110013");
        for (String operation : new String[]{"/paths/~1v1~1files/post", "/paths/~1v1~1files~1batch/post", "/paths/~1v1~1files/delete"}) {
            assertThat(api.at(operation + "/description").asText()).contains("system:file:manage");
        }
        assertThat(api.at("/paths/~1v1~1me~1github~1complete/post/requestBody/required").asBoolean()).isTrue();
        assertThat(api.at("/paths/~1v1~1auth~1github~1complete/post/responses/200/content/application~1json/schema/$ref").asText())
                .contains("ResultLoginResponse");
        assertThat(api.at("/paths/~1v1~1auth~1github~1callback/get/parameters").toString())
                .doesNotContain("token", "Authorization", "clientSecret");
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target/github-login-openapi.json"), document);
    }
}
