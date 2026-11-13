package com.admin.template.controller.auth;

import com.admin.template.AdminTemplateApplication;
import com.admin.template.common.exception.AppException;
import com.admin.template.common.exception.SystemError;
import com.admin.template.service.auth.GitHubOAuthClient;
import com.admin.template.service.auth.GitHubOAuthService;
import com.admin.template.mapper.UserOAuthAccountMapper;
import com.admin.template.entity.UserOAuthAccountEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.admin.template.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = AdminTemplateApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:github-contract;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "app.auth.github.enabled=true", "app.auth.github.client-id=client", "app.auth.github.client-secret=private-client-secret",
        "app.auth.github.redirect-uri=https://app.example/api/v1/auth/github/callback",
        "app.auth.github.frontend-redirect-uri=https://app.example/#/oauth/github"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class GitHubOAuthIntegrationTest {
    @TempDir static Path publicDirectory;

    @DynamicPropertySource
    static void uploadDirectory(DynamicPropertyRegistry registry) {
        registry.add("app.upload.directory", () -> publicDirectory.toString());
    }

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JwtService jwt;
    @Autowired private ObjectMapper json;
    @Autowired private GitHubOAuthService github;
    @MockBean private GitHubOAuthClient client;
    @SpyBean private UserOAuthAccountMapper accounts;
    private static final AtomicInteger IPS = new AtomicInteger();
    private MockHttpSession session;
    private String ip;

    @BeforeEach
    void prepareDatabase() {
        ip = "192.0.2." + IPS.incrementAndGet();
        session = new MockHttpSession();
        jdbc.execute("CREATE TABLE IF NOT EXISTS users (id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) UNIQUE, "
                + "password VARCHAR(100), nickname VARCHAR(50), email VARCHAR(100), phone VARCHAR(20), avatar VARCHAR(255), "
                + "status BOOLEAN, last_login TIMESTAMP, last_logout TIMESTAMP, created_at TIMESTAMP, updated_at TIMESTAMP, deleted_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS roles (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50), code VARCHAR(50), "
                + "description VARCHAR(255), status BOOLEAN, sort INT, created_at TIMESTAMP, updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS menus (menu_id VARCHAR(50), buttonPermission VARCHAR(100), status BOOLEAN)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS user_roles (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, role_id BIGINT, "
                + "created_at TIMESTAMP, updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS role_menus (role_id BIGINT, menu_id VARCHAR(50))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS user_oauth_accounts (id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT NOT NULL, "
                + "provider VARCHAR(20) NOT NULL, provider_user_id VARCHAR(100) NOT NULL, provider_username VARCHAR(100) NOT NULL, "
                + "created_at TIMESTAMP, updated_at TIMESTAMP, UNIQUE(provider,provider_user_id), UNIQUE(user_id,provider), "
                + "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE)");
        for (String table : List.of("user_oauth_accounts", "user_roles", "role_menus", "menus", "roles", "users")) jdbc.update("DELETE FROM " + table);
        jdbc.update("INSERT INTO users(id,username,password,status) VALUES(7,'alice','legacy-password',true),(8,'bob','legacy-password',true)");
        jdbc.update("INSERT INTO roles(id,name,code,status) VALUES(1,'Operator','operator',true),(2,'User','user',true)");
        jdbc.update("INSERT INTO menus(menu_id,buttonPermission,status) VALUES('home','read',true)");
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(7,1)");
        jdbc.update("INSERT INTO role_menus(role_id,menu_id) VALUES(1,'home'),(2,'home')");
        when(client.fetchProfile(any(), any())).thenReturn(new GitHubOAuthClient.Profile("42", "octocat"));
    }

    @Test
    void boundIdentityUsesExistingAccountChecksAuditAndLogoutTokenSemantics() throws Exception {
        bindRow(7, "42");
        String oldToken = bearer(7);
        mvc.perform(post("/v1/auth/logout").header("Authorization", oldToken)).andExpect(jsonPath("$.code").value(0));
        String state = start(null);
        callback(state, "private-code", null);
        var login = complete(state, null, 200, 0);
        assertThat(login.at("/data/initialPasswordFlag").asBoolean()).isTrue();
        String token = login.at("/data/token").asText();
        var cutoff = jdbc.queryForObject("SELECT last_logout FROM users WHERE id=7", java.sql.Timestamp.class).toInstant();
        assertThat(jwt.parseTokenDetails(token).issuedAt()).isAfter(cutoff);
        assertThat(jdbc.queryForObject("SELECT last_login FROM users WHERE id=7", java.sql.Timestamp.class)).isNotNull();
        mvc.perform(get("/v1/me/github").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.bound").value(true)).andExpect(jsonPath("$.data.login").value("octocat"));
        mvc.perform(get("/v1/me/github").session(session)).andExpect(jsonPath("$.code").value(110005));
        complete(state, null, 200, 110009);
    }

    @Test
    void rejectsDisabledDeletedNoRoleAndNoMenuBoundAccountsWithoutRegisteringAgain() throws Exception {
        bindRow(7, "42");
        jdbc.update("UPDATE users SET status=false WHERE id=7");
        completeAfterCallback(null, 110003);
        jdbc.update("UPDATE users SET status=true,deleted_at=CURRENT_TIMESTAMP WHERE id=7");
        completeAfterCallback(null, 120001);
        jdbc.update("UPDATE users SET deleted_at=null WHERE id=7");
        jdbc.update("DELETE FROM user_roles");
        completeAfterCallback(null, 110006);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(7,1)");
        jdbc.update("DELETE FROM role_menus");
        completeAfterCallback(null, 110007);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_oauth_accounts", Integer.class)).isEqualTo(1);
    }

    @Test
    void firstAuthorizationRegistersAnIndependentPasswordlessOrdinaryUserAndRepeatsUseStableId() throws Exception {
        when(client.fetchProfile(any(), any())).thenReturn(new GitHubOAuthClient.Profile("42", "alice"));
        var first = login();
        var identity = jwt.parseToken(first.at("/data/token").asText());
        long userId = identity.id();
        assertThat(userId).isNotIn(7L, 8L);
        assertThat(identity.roles()).containsExactly("user");
        var user = jdbc.queryForMap("SELECT * FROM users WHERE id=?", userId);
        assertThat((String) user.get("username")).hasSizeLessThanOrEqualTo(50).isNotEqualTo("alice").isNotEqualTo("42");
        assertThat(user.get("nickname")).isEqualTo("alice");
        assertThat(user.get("password")).isNull();
        assertThat(user.get("status")).isEqualTo(true);
        assertThat(user.get("created_at")).isNotNull();
        assertThat(user.get("updated_at")).isNotNull();
        assertThat(user.get("last_login")).isNotNull();
        assertThat(jdbc.queryForObject("SELECT role_id FROM user_roles WHERE user_id=?", Long.class, userId)).isEqualTo(2);
        when(client.fetchProfile(any(), any())).thenReturn(new GitHubOAuthClient.Profile("42", "renamed"));
        assertThat(jwt.parseToken(login().at("/data/token").asText()).id()).isEqualTo(userId);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_oauth_accounts", Integer.class)).isEqualTo(1);
        mvc.perform(get("/v1/users").header("Authorization", "Bearer " + first.at("/data/token").asText()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(110004));
    }

    @Test
    void newlyRegisteredUserCannotUploadASpoofedHtmlFileToThePublicDirectory() throws Exception {
        String token = login().at("/data/token").asText();
        var file = new MockMultipartFile("file", "spoof.html", "image/png", "<script>alert(1)</script>".getBytes());
        mvc.perform(multipart("/v1/files").file(file).param("path", "denied-single")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(110004));
        assertThat(publicDirectory.resolve("denied-single")).doesNotExist();
    }

    @Test
    void newlyRegisteredUserCannotBatchUploadToThePublicDirectory() throws Exception {
        String token = login().at("/data/token").asText();
        var file = new MockMultipartFile("files", "spoof.html", "image/png", "<script>alert(1)</script>".getBytes());
        mvc.perform(multipart("/v1/files/batch").file(file).param("path", "denied-batch")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(110004));
        assertThat(publicDirectory.resolve("denied-batch")).doesNotExist();
    }

    @Test
    void newlyRegisteredUserCannotDeleteAKnownPublicFile() throws Exception {
        String token = login().at("/data/token").asText();
        Path existingFile = publicDirectory.resolve("other-user.pdf");
        Files.writeString(existingFile, "existing-public-file");
        mvc.perform(delete("/v1/files").contentType("application/json")
                        .content("{\"filePath\":\"/public/other-user.pdf\"}").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(110004));
        assertThat(existingFile).hasContent("existing-public-file");
    }

    @Test
    void superAdminAndASpecificallyFilePermittedRoleCanUploadBatchAndDelete() throws Exception {
        jdbc.update("UPDATE roles SET code='super_admin' WHERE id=1");
        jdbc.update("INSERT INTO roles(id,name,code,status) VALUES(3,'File manager','file_manager',true)");
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(8,3)");
        jdbc.update("INSERT INTO menus(menu_id,buttonPermission,status) VALUES('file-write','system:file:manage',true)");
        jdbc.update("INSERT INTO role_menus(role_id,menu_id) VALUES(3,'file-write')");
        for (long userId : new long[]{7, 8}) {
            String token = "Bearer " + jwt.createToken(userId, "file-writer", List.of("stale-jwt-role"));
            var single = mvc.perform(multipart("/v1/files")
                            .file(new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1}))
                            .header("Authorization", token))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0)).andReturn();
            String url = json.readTree(single.getResponse().getContentAsString()).at("/data/url").asText();
            Path uploaded = publicDirectory.resolve(url.substring("/public/".length()));
            assertThat(uploaded).hasBinaryContent(new byte[]{1});
            mvc.perform(multipart("/v1/files/batch")
                            .file(new MockMultipartFile("files", "avatar.png", "image/png", new byte[]{2}))
                            .header("Authorization", token))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.length()").value(1));
            mvc.perform(delete("/v1/files").contentType("application/json")
                            .content(json.writeValueAsString(new com.admin.template.dto.file.DeleteFileRequest(url)))
                            .header("Authorization", token))
                    .andExpect(status().isNoContent()).andExpect(content().string(""));
            assertThat(uploaded).doesNotExist();
        }
    }

    @Test
    void registrationRollsBackWhenTheDefaultRoleIsMissingDisabledOrHasNoMenus() throws Exception {
        jdbc.update("DELETE FROM roles WHERE id=2");
        completeAfterCallback(null, 110006);
        assertRegistrationRolledBack();
        jdbc.update("INSERT INTO roles(id,name,code,status) VALUES(2,'User','user',false)");
        completeAfterCallback(null, 110006);
        assertRegistrationRolledBack();
        jdbc.update("UPDATE roles SET status=true WHERE id=2");
        jdbc.update("DELETE FROM role_menus WHERE role_id=2");
        completeAfterCallback(null, 110007);
        assertRegistrationRolledBack();
    }

    @Test
    void simultaneousFirstAuthorizationsReturnTheWinnerAndDoNotLeaveAnOrphanUser() throws Exception {
        var firstSession = new MockHttpSession();
        var secondSession = new MockHttpSession();
        var first = github.authorize(firstSession, null);
        var second = github.authorize(secondSession, null);
        github.callback(firstSession, "code-one", first.state(), null);
        github.callback(secondSession, "code-two", second.state(), null);
        var initialReads = new AtomicInteger();
        var barrier = new CyclicBarrier(2);
        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            if (initialReads.incrementAndGet() <= 2) barrier.await(10, TimeUnit.SECONDS);
            return result;
        }).when(accounts).selectOne(any(QueryWrapper.class));
        try (var executor = Executors.newFixedThreadPool(2)) {
            var one = executor.submit(() -> github.completeLogin(firstSession, first.state()));
            var two = executor.submit(() -> github.completeLogin(secondSession, second.state()));
            long userId = jwt.parseToken(one.get(15, TimeUnit.SECONDS).token()).id();
            assertThat(jwt.parseToken(two.get(15, TimeUnit.SECONDS).token()).id()).isEqualTo(userId);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_roles", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_oauth_accounts", Integer.class)).isEqualTo(1);
    }

    @Test
    void passwordlessOrBlankPasswordAccountsCannotUnbindTheirOnlyLoginMethod() throws Exception {
        bindRow(7, "42");
        for (String password : new String[]{null, "", "  "}) {
            jdbc.update("UPDATE users SET password=? WHERE id=7", password);
            mvc.perform(delete("/v1/me/github").header("Authorization", bearer(7)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(110013))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("GitHub")));
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_oauth_accounts", Integer.class)).isEqualTo(1);
        }
    }

    private JsonNode login() throws Exception {
        String state = start(null);
        callback(state, "private-code", null);
        return complete(state, null, 200, 0);
    }

    private void assertRegistrationRolledBack() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_roles", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_oauth_accounts", Integer.class)).isZero();
    }

    @Test
    void bindsOnlyForTheInitiatingJwtUserAndUnbindsOnlyTheirAccount() throws Exception {
        String initialSessionId = session.getId();
        String state = start(bearer(7));
        assertThat(session.getId()).isNotEqualTo(initialSessionId);
        callback(state, "private-code", null);
        complete(state, bearer(8), 200, 110009);
        complete(state, bearer(7), 200, 0);
        assertThat(jdbc.queryForObject("SELECT user_id FROM user_oauth_accounts WHERE provider_user_id='42'", Long.class)).isEqualTo(7);
        mvc.perform(delete("/v1/me/github").header("Authorization", bearer(8)))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_oauth_accounts", Integer.class)).isEqualTo(1);
        mvc.perform(delete("/v1/me/github").header("Authorization", bearer(7)))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(get("/v1/me/github").header("Authorization", bearer(7)))
                .andExpect(jsonPath("$.data.bound").value(false)).andExpect(jsonPath("$.data.login").isEmpty());
    }

    @Test
    void preventsBothKindsOfBindingTakeoverAndEnforcesDatabaseUniqueness() throws Exception {
        bindRow(8, "42");
        completeAfterCallback(bearer(7), 110012);
        assertThat(jdbc.queryForObject("SELECT user_id FROM user_oauth_accounts", Long.class)).isEqualTo(8);
        assertThatThrownBy(() -> bindRow(7, "42")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> bindRow(8, "43")).isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update("DELETE FROM user_oauth_accounts");
        bindRow(7, "43");
        completeAfterCallback(bearer(7), 110012);
        assertThat(jdbc.queryForObject("SELECT provider_user_id FROM user_oauth_accounts", String.class)).isEqualTo("43");
    }

    @Test
    void callbackNeverRedirectsSecretsAndProviderFailuresAreRedacted(CapturedOutput output) throws Exception {
        String denied = start(null);
        callback(denied, null, "access_denied private-query-error");
        complete(denied, null, 200, 110010);
        String failed = start(null);
        when(client.fetchProfile(any(), any())).thenThrow(new AppException(SystemError.GITHUB_SERVICE_ERROR, null, null));
        callback(failed, "private-code", null);
        var result = complete(failed, null, 502, 170001);
        assertThat(result.toString()).doesNotContain("private-code", "private-access-token", "private-client-secret");
        assertThat(output.getAll()).doesNotContain("private-code", "private-access-token", "private-client-secret", "private-query-error");
    }

    @Test
    void reusesLoginRateLimitsBeforeOAuthBodyValidation() throws Exception {
        for (int attempt = 0; attempt < 10; attempt++) {
            mvc.perform(post("/v1/auth/github/complete").with(r -> {r.setRemoteAddr(ip);return r;})
                    .contentType("application/json").content("{}"))
                    .andExpect(jsonPath("$.code").value(100001));
        }
        mvc.perform(post("/v1/me/github/authorize").header("Authorization", bearer(7))
                .with(r -> {r.setRemoteAddr(ip);return r;}))
                .andExpect(status().isOk()).andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value(100002));
    }

    private void bindRow(long user, String providerId) {
        jdbc.update("INSERT INTO user_oauth_accounts(user_id,provider,provider_user_id,provider_username) VALUES(?,'github',?,'octocat')", user, providerId);
    }

    private String bearer(long user) { return "Bearer " + jwt.createToken(user, "user-" + user, List.of("operator")); }

    private String start(String auth) throws Exception {
        var request = post(auth == null ? "/v1/auth/github/authorize" : "/v1/me/github/authorize")
                .session(session).with(r -> {r.setRemoteAddr(ip);return r;});
        if (auth != null) request.header("Authorization", auth);
        var response = mvc.perform(request).andExpect(jsonPath("$.code").value(0))
                .andExpect(header().string("Cache-Control", "no-store")).andReturn();
        return json.readTree(response.getResponse().getContentAsString()).at("/data/state").asText();
    }

    private void callback(String state, String code, String error) throws Exception {
        var request = get("/v1/auth/github/callback").session(session).param("state", state);
        if (code != null) request.param("code", code);
        if (error != null) request.param("error", error);
        mvc.perform(request).andExpect(status().isFound()).andExpect(redirectedUrl("https://app.example/#/oauth/github"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));
    }

    private JsonNode complete(String state, String auth, int statusCode, int code) throws Exception {
        var request = post(auth == null ? "/v1/auth/github/complete" : "/v1/me/github/complete").session(session)
                .with(r -> {r.setRemoteAddr(ip);return r;}).contentType("application/json").content("{\"state\":\"" + state + "\"}");
        if (auth != null) request.header("Authorization", auth);
        return json.readTree(mvc.perform(request).andExpect(status().is(statusCode)).andExpect(jsonPath("$.code").value(code))
                .andReturn().getResponse().getContentAsString());
    }

    private void completeAfterCallback(String auth, int code) throws Exception {
        String state = start(auth);
        callback(state, "private-code", null);
        complete(state, auth, 200, code);
    }
}
