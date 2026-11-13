package com.admin.template.config;

import com.admin.template.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mock.web.MockServletContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitHubOAuthPropertiesTest {
    @Test
    void resolvesExactDocumentedEnvironmentNamesWithoutApplicationYaml() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new SystemEnvironmentPropertySource("github-env", Map.of(
                    "APP_AUTH_GITHUB_ENABLED", "true", "APP_AUTH_GITHUB_CLIENT_ID", "env-client",
                    "APP_AUTH_GITHUB_CLIENT_SECRET", "env-secret",
                    "APP_AUTH_GITHUB_REDIRECT_URI", "https://app.example/api/v1/auth/github/callback",
                    "APP_AUTH_GITHUB_FRONTEND_REDIRECT_URI", "https://app.example/#/oauth/github")));
            context.register(GitHubOAuthProperties.class);
            context.refresh();
            var properties = context.getBean(GitHubOAuthProperties.class);
            properties.requireEnabled();
            assertThat(properties.enabled()).isTrue();
            assertThat(properties.clientId()).isEqualTo("env-client");
            assertThat(properties.clientSecret()).isEqualTo("env-secret");
            assertThat(properties.toString()).doesNotContain("env-secret", "env-client");
        }
    }

    @Test
    void startsDisabledWithNoSecretsAndRejectsUnsafeOrCrossOriginRedirects() {
        try (var context = new AnnotationConfigApplicationContext(GitHubOAuthProperties.class)) {
            assertThat(context.getBean(GitHubOAuthProperties.class).enabled()).isFalse();
        }
        for (String callback : new String[]{"", "javascript:alert(1)", "https://user:password@app.example/callback",
                "https://app.example:444/callback", "https://other.example/callback", "https://app.example/callback?next=x",
                "https://app.example/callback#fragment"}) {
            var properties = new GitHubOAuthProperties(true, "id", "secret", callback, "https://app.example/#/oauth/github");
            assertThatThrownBy(properties::requireEnabled).isInstanceOf(BizException.class);
        }
        new GitHubOAuthProperties(true, "id", "secret", "https://app.example:443/callback", "https://app.example/#/oauth/github")
                .requireEnabled();
    }

    @Test
    void hardensTemporarySessionCookiesAndForcesHttpsSecureBehindAProxy() throws Exception {
        var context = new MockServletContext();
        var properties = new GitHubOAuthProperties(true, "id", "secret", "https://app.example/callback", "https://app.example/#/oauth/github");
        new GitHubSessionConfiguration().githubSessionCookies(properties).onStartup(context);
        var cookie = context.getSessionCookieConfig();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax");
        assertThat(context.getEffectiveSessionTrackingModes()).containsExactly(jakarta.servlet.SessionTrackingMode.COOKIE);
    }
}
