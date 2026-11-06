package com.admin.template.controller.file;

import com.admin.template.AdminTemplateApplication;
import com.admin.template.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AdminTemplateApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "unit-test"})
class MultipartUploadContractTest {
    @TempDir
    static Path publicDirectory;

    @DynamicPropertySource
    static void uploadDirectory(DynamicPropertyRegistry registry) {
        registry.add("app.upload.directory", () -> publicDirectory.resolve("public").toString());
    }

    @LocalServerPort
    private int port;

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
    private ObjectMapper objectMapper;

    @Test
    void acceptsTenFilesAtTheTenMebibyteLimitIncludingMultipartOverhead() throws Exception {
        String boundary = "JavaMigrationFileBoundary";
        byte[] bytes = new byte[10 * 1024 * 1024];
        List<HttpRequest.BodyPublisher> parts = new ArrayList<>();
        for (int file = 0; file < 10; file++) {
            String header = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"files\"; filename=\"part-" + file + ".png\"\r\n"
                    + "Content-Type: image/png\r\n\r\n";
            parts.add(HttpRequest.BodyPublishers.ofString(header, StandardCharsets.UTF_8));
            parts.add(HttpRequest.BodyPublishers.ofByteArray(bytes));
            parts.add(HttpRequest.BodyPublishers.ofString("\r\n"));
        }
        parts.add(HttpRequest.BodyPublishers.ofString("--" + boundary + "--\r\n"));
        String token = jwtService.createToken(7L, "uploader", List.of("operator"));
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v1/files/batch"))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.concat(parts.toArray(HttpRequest.BodyPublisher[]::new)))
                .build();

        HttpResponse<String> response;
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(objectMapper.readTree(response.body()).path("data")).hasSize(10);
    }
}
