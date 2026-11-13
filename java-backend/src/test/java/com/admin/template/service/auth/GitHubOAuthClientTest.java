package com.admin.template.service.auth;

import com.admin.template.common.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GitHubOAuthClientTest {
    private MockRestServiceServer server;
    private GitHubOAuthClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GitHubOAuthClient(GitHubOAuthServiceTest.properties(), builder.build());
    }

    @Test
    void exchangesTheCodeAndVerifierAndReadsOnlyStablePublicIdentity() {
        server.expect(requestTo("https://github.com/login/oauth/access_token"))
                .andExpect(method(HttpMethod.POST)).andExpect(header("Accept", "application/json"))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(org.hamcrest.Matchers.containsString("code_verifier=verifier"),
                        org.hamcrest.Matchers.containsString("client_secret=secret"))))
                .andRespond(withSuccess("{\"access_token\":\"internal-token\",\"token_type\":\"bearer\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.github.com/user")).andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer internal-token"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andExpect(header("User-Agent", "admin-template"))
                .andRespond(withSuccess("{\"id\":42,\"login\":\"octocat\",\"email\":\"ignored@example.com\"}", MediaType.APPLICATION_JSON));
        assertThat(client.fetchProfile("code", "verifier")).isEqualTo(new GitHubOAuthClient.Profile("42", "octocat"));
        server.verify();
    }

    @Test
    void rejectsTokenEndpointErrorsWithoutLeakingTheResponseOrCause() {
        server.expect(anything()).andRespond(withSuccess("{\"error\":\"bad_verification_code\",\"error_description\":\"token=secret\"}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.fetchProfile("private-code", "verifier")).isInstanceOf(AppException.class)
                .hasMessageNotContaining("secret").hasMessageNotContaining("private-code").hasCause(null);
        server.verify();
    }

    @Test
    void rejectsUpstreamHttpErrorsAndMalformedPublicIdentity() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.BAD_GATEWAY).body("private access_token"));
        assertThatThrownBy(() -> client.fetchProfile("code", "verifier")).isInstanceOf(AppException.class).hasCause(null);
        server.verify();
    }

    @Test
    void redactsNetworkExceptionCausesAndRejectsNonNumericIdentity() {
        server.expect(anything()).andRespond(withException(new IOException("private-code private-access-token private-client-secret")));
        assertThatThrownBy(() -> client.fetchProfile("private-code", "verifier")).isInstanceOf(AppException.class)
                .hasMessageNotContaining("private-code").hasCause(null);
        server.verify();
        server.reset();
        server.expect(anything()).andRespond(withSuccess("{\"access_token\":\"internal\",\"token_type\":\"bearer\"}", MediaType.APPLICATION_JSON));
        server.expect(anything()).andRespond(withSuccess("{\"id\":\"42\",\"login\":\"octocat\"}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.fetchProfile("code", "verifier")).isInstanceOf(AppException.class).hasCause(null);
        server.verify();
    }
}
