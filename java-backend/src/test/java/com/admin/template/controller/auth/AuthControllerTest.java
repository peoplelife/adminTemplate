package com.admin.template.controller.auth;

import com.admin.template.dto.auth.LoginResponse;
import com.admin.template.common.exception.BizException;
import com.admin.template.common.exception.BusinessError;
import com.admin.template.security.JwtService;
import com.admin.template.service.auth.AuthService;
import com.admin.template.AdminTemplateApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = AdminTemplateApplication.class)
@ActiveProfiles({"test", "unit-test"})
@AutoConfigureMockMvc
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

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
    void returnsExplicitLoginResultWithTheRequestId() throws Exception {
        when(authService.login("alice", "secret123")).thenReturn(new LoginResponse("test-token", false));

        mockMvc.perform(post("/v1/auth/login").header("X-Trace-Id", "login-contract")
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.20");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", "login-contract"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").value("test-token"))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.traceId").value("login-contract"))
                .andExpect(jsonPath("$.details").value(nullValue()));
    }

    @Test
    void returns401AndTheBusinessCodeForInvalidCredentials() throws Exception {
        when(authService.login("alice", "wrong-password"))
                .thenThrow(new BizException(BusinessError.INVALID_CREDENTIALS));

        mockMvc.perform(post("/v1/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.21");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(110001))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void returnsExplicitLogoutResultForAnAuthenticatedUser() throws Exception {
        String token = jwtService.createToken(7L, "alice", java.util.List.of("operator"));

        mockMvc.perform(post("/v1/auth/logout").header("Authorization", "Bearer " + token)
                        .header("X-Trace-Id", "logout-contract"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.message").value("退出登录成功"))
                .andExpect(jsonPath("$.traceId").value("logout-contract"));
    }

    @Test
    void rejectsLoginWithoutUsernameAndPassword() throws Exception {
        mockMvc.perform(post("/v1/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.10");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100001))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void countsInvalidLoginAttemptsBeforeValidation() throws Exception {
        for (int attempt = 0; attempt < 10; attempt++) {
            mockMvc.perform(post("/v1/auth/login")
                            .with(request -> {
                                request.setRemoteAddr("203.0.113.11");
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/v1/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.11");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value(100002))
                .andExpect(jsonPath("$.message").value("请求过于频繁，请稍后重试"));
    }

    @Test
    void returnsRetryAfterWhenIpExceedsTenLoginAttempts() throws Exception {
        when(authService.login("alice", "secret123"))
                .thenReturn(new com.admin.template.dto.auth.LoginResponse("token", false));
        String body = "{\"username\":\"alice\",\"password\":\"secret123\"}";

        for (int attempt = 0; attempt < 10; attempt++) {
            mockMvc.perform(post("/v1/auth/login")
                            .with(request -> {
                                request.setRemoteAddr("203.0.113.12");
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/v1/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.12");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value(100002));
    }
}
