package com.admin.template.controller.file;

import com.admin.template.AdminTemplateApplication;
import com.admin.template.security.JwtService;
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
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AdminTemplateApplication.class)
@ActiveProfiles({"test", "unit-test"})
@AutoConfigureMockMvc
class FileControllerTest {
    @TempDir
    static Path publicDirectory;

    @DynamicPropertySource
    static void uploadDirectory(DynamicPropertyRegistry registry) {
        registry.add("app.upload.directory", () -> publicDirectory.resolve("public").toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Autowired
    private JwtService jwtService;
    @org.springframework.boot.test.mock.mockito.MockBean
    private com.admin.template.mapper.UserMapper authenticationUsers;
    @org.springframework.boot.test.mock.mockito.MockBean
    private com.admin.template.mapper.UserRoleMapper userRoles;
    @org.springframework.boot.test.mock.mockito.MockBean
    private com.admin.template.mapper.MenuMapper menus;

    @org.junit.jupiter.api.BeforeEach
    void setUpAuthenticationAccount() {
        org.mockito.Mockito.when(userRoles.selectActiveRolesByUserId(7L))
                .thenReturn(List.of(new com.admin.template.dto.security.ActiveRole(3L, "file_manager", "File manager")));
        org.mockito.Mockito.when(menus.selectActivePermissionsForRoleIds(List.of(3L)))
                .thenReturn(List.of(new com.admin.template.dto.security.MenuPermission("file-write", "system:file:manage")));
        org.mockito.Mockito.when(authenticationUsers.selectById(org.mockito.ArgumentMatchers.anyLong()))
                .thenAnswer(invocation -> {
                    var user = new com.admin.template.entity.UserEntity();
                    user.id = invocation.getArgument(0);
                    user.status = true;
                    return user;
                });
    }

    @Test
    void requiresAuthenticationForSingleFileUpload() throws Exception {
        mockMvc.perform(post("/v1/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(110005));
    }

    @Test
    void rejectsMissingSingleAndBatchUploadsWithSourceValidationEnvelope() throws Exception {
        String token = jwtService.createToken(7L, "uploader", List.of("operator"));

        for (String path : new String[]{"/v1/files", "/v1/files/batch"}) {
            mockMvc.perform(multipart(path).header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(100001))
                    .andExpect(jsonPath("$.message").value("请选择要上传的文件"))
                    .andExpect(jsonPath("$.details").value(nullValue()));
        }
    }

    @Test
    void uploadsAnAllowedZeroByteFile() throws Exception {
        String token = jwtService.createToken(7L, "uploader", List.of("operator"));
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        mockMvc.perform(multipart("/v1/files").file(file).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(0))
                .andExpect(jsonPath("$.data.mimetype").value("image/png"));
    }

    @Test
    void rejectsMultipleFilesOnTheSingleUploadRoute() throws Exception {
        String token = jwtService.createToken(7L, "uploader", List.of("operator"));
        MockMultipartFile first = new MockMultipartFile("file", "first.png", "image/png", new byte[]{1});
        MockMultipartFile second = new MockMultipartFile("file", "second.png", "image/png", new byte[]{2});

        mockMvc.perform(multipart("/v1/files").file(first).file(second).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100001))
                .andExpect(jsonPath("$.message").value("文件数量超过限制（最多10个）"));
    }

    @Test
    void rejectsMoreThanTenBatchFilesUsingTheSourceCountError() throws Exception {
        String token = jwtService.createToken(7L, "uploader", List.of("operator"));
        MockMultipartHttpServletRequestBuilder request = multipart("/v1/files/batch");
        for (int index = 0; index < 11; index++) {
            request.file(new MockMultipartFile("files", "photo-" + index + ".png", "image/png", new byte[]{1}));
        }
        request.header("Authorization", "Bearer " + token);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("文件数量超过限制（最多10个）"));
    }

    @Test
    void registersUploadAndDeleteRoutes() {
        assertThat(handlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(info -> info.getPatternValues().stream()).toList())
                .contains("/v1/files", "/v1/files/batch");
    }
}
