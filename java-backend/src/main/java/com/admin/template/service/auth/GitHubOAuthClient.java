package com.admin.template.service.auth;

import com.admin.template.common.exception.AppException;
import com.admin.template.common.exception.SystemError;
import com.admin.template.config.GitHubOAuthProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class GitHubOAuthClient {
    private final GitHubOAuthProperties properties;
    private final RestClient client;

    @Autowired
    public GitHubOAuthClient(GitHubOAuthProperties properties) {
        this.properties = properties;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.client = RestClient.builder().requestFactory(factory).build();
    }

    GitHubOAuthClient(GitHubOAuthProperties properties, RestClient client) {
        this.properties = properties;
        this.client = client;
    }

    public Profile fetchProfile(String code, String verifier) {
        try {
            var form = new LinkedMultiValueMap<String, String>();
            form.add("client_id", properties.clientId());
            form.add("client_secret", properties.clientSecret());
            form.add("redirect_uri", properties.redirectUri());
            form.add("code", code);
            form.add("code_verifier", verifier);
            JsonNode token = client.post().uri("https://github.com/login/oauth/access_token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.APPLICATION_JSON)
                    .body(form).retrieve().body(JsonNode.class);
            if (token == null || token.has("error") || !token.path("access_token").isTextual()
                    || token.path("access_token").asText().isBlank()
                    || !"bearer".equalsIgnoreCase(token.path("token_type").asText())) throw failure();
            JsonNode profile = client.get().uri("https://api.github.com/user")
                    .header("Authorization", "Bearer " + token.path("access_token").asText())
                    .header("X-GitHub-Api-Version", "2022-11-28").header("User-Agent", "admin-template")
                    .accept(MediaType.APPLICATION_JSON).retrieve().body(JsonNode.class);
            if (profile == null || !profile.path("id").isIntegralNumber() || !profile.path("id").canConvertToLong()
                    || profile.path("id").asLong() <= 0 || !profile.path("login").isTextual()
                    || !profile.path("login").asText().matches("[A-Za-z0-9-]{1,100}")) throw failure();
            return new Profile(profile.path("id").asText(), profile.path("login").asText());
        } catch (RestClientException | IllegalArgumentException exception) {
            // HTTP exception causes can contain codes, access tokens or response bodies.
            throw failure();
        }
    }

    private static AppException failure() {
        return new AppException(SystemError.GITHUB_SERVICE_ERROR, null, null);
    }

    public record Profile(String id, String login) {}
}
