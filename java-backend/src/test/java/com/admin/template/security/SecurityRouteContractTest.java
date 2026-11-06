package com.admin.template.security;

import com.admin.template.AdminTemplateApplication;
import com.admin.template.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AdminTemplateApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(SecurityRouteContractTest.NewModuleController.class)
class SecurityRouteContractTest {
    @RestController
    static class NewModuleController {
        @GetMapping("/v1/new-module")
        public String newModule() { return "authenticated"; }
    }
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

    @Test
    void leavesUnknownAndExcludedAiRoutesAsNotFound() throws Exception {
        String token = jwtService.createToken(7L, "chat-user", java.util.List.of("operator"));
        for (String path : new String[]{"/v1/not-found", "/v1/chats", "/v1/knowledge-bases"}) {
            mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(404));
        }

        mockMvc.perform(get("/v1/ai/not-found").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void preservesNullDetailsForMissingBearerAuthentication() throws Exception {
        mockMvc.perform(get("/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(110005))
                .andExpect(jsonPath("$.details").value(nullValue()));
    }

    @Test
    void preservesNullDetailsForInvalidBearerAuthentication() throws Exception {
        mockMvc.perform(get("/v1/roles").header("Authorization", "Bearer invalid-token")
                        .header("X-Trace-Id", "invalid-token-contract"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(110002))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.traceId").value("invalid-token-contract"))
                .andExpect(jsonPath("$.details").value(nullValue()));
    }

    @Test
    void requiresAuthenticationForUnknownApplicationRoutes() throws Exception {
        mockMvc.perform(get("/v1/new-module"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(110005));
    }

    @Test
    void authenticatesNewModuleWithoutAddingItsPathToSecurityConfiguration() throws Exception {
        String token = jwtService.createToken(7L, "module-user", java.util.List.of("operator"));
        mockMvc.perform(get("/v1/new-module").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string("authenticated"));
        mockMvc.perform(get("/v1/new-module").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(110002));
    }

    @Test
    void preservesPublicEndpointsAndDoesNotWhitelistSimilarPrefixes() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/docs")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.openapi").exists());
        for (String path : new String[]{"/publicity", "/docs-private", "/v1/auth/login-extra", "/another-module"}) {
            mockMvc.perform(get(path)).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(110005));
        }
    }
}
