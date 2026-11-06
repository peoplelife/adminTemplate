package com.admin.template.controller.user;

import com.admin.template.service.user.UserExportService;
import com.admin.template.service.user.UserService;
import com.admin.template.AdminTemplateApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.admin.template.security.JwtService;
import com.admin.template.security.PermissionService;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@SpringBootTest(classes = AdminTemplateApplication.class)
@ActiveProfiles({"test", "unit-test"})
@AutoConfigureMockMvc
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

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

    @MockBean
    private UserService userService;

    @MockBean
    private UserExportService exportService;

    @MockBean
    private PermissionService permissionService;

    @Test
    void requiresAuthenticationForUserList() throws Exception {
        mockMvc.perform(get("/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(110005))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void registersCurrentUserAndCrudRoutes() {
        assertThat(handlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(info -> info.getPatternValues().stream())
                .toList()).contains("/v1/me", "/v1/users", "/v1/users/{id}");
    }

    @Test
    void servesUserExportAsRawXlsxWithoutJsonEnvelope() throws Exception {
        byte[] xlsx = new byte[]{'P', 'K', 3, 4};
        when(userService.getExportData(null, null)).thenReturn(java.util.List.of());
        when(exportService.createUserListBuffer(java.util.List.of())).thenReturn(xlsx);
        String token = jwtService.createToken(1, "root", java.util.List.of("super_admin"));

        mockMvc.perform(get("/v1/users/export").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment; filename*=UTF-8''")))
                .andExpect(content().bytes(xlsx));
    }
}
