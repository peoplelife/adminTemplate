package com.admin.template.common;

import com.admin.template.AdminTemplateApplication;
import com.admin.template.common.exception.BizException;
import com.admin.template.common.exception.BusinessError;
import com.admin.template.dto.auth.LoginResponse;
import com.admin.template.service.auth.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AdminTemplateApplication.class)
@ActiveProfiles({"test", "unit-test"})
@AutoConfigureMockMvc
class NumericResultContractTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void loginSuccessUsesNumericCodeZero() throws Exception {
        when(authService.login("alice", "secret123")).thenReturn(new LoginResponse("test-token", false));
        mockMvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void invalidCredentialsAreABusinessFailureWithHttp200() throws Exception {
        when(authService.login("alice", "wrong-password"))
                .thenThrow(new BizException(BusinessError.INVALID_CREDENTIALS));
        mockMvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(110001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void disabledAccountHasItsOwnBusinessCode() throws Exception {
        when(authService.login("alice", "secret123"))
                .thenThrow(new BizException(BusinessError.ACCOUNT_DISABLED));
        mockMvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(110003))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void businessErrorCodesAreUniqueAndDoNotUseHttpStatusValues() {
        int[] codes = Arrays.stream(BusinessError.values()).mapToInt(BusinessError::code).toArray();
        assertThat(codes).doesNotHaveDuplicates();
        assertThat(Arrays.stream(codes).allMatch(code -> code > 999)).isTrue();
    }

    @Test
    void invalidInputIsABusinessFailureWithHttp200() throws Exception {
        mockMvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100001))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void missingAndInvalidTokensAreSeparateBusinessFailures() throws Exception {
        mockMvc.perform(get("/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(110005))
                .andExpect(jsonPath("$.success").value(false));
        mockMvc.perform(get("/v1/users").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(110002))
                .andExpect(jsonPath("$.success").value(false));
    }
}
