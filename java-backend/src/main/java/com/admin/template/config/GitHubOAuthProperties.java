package com.admin.template.config;

import com.admin.template.common.exception.BizException;
import com.admin.template.common.exception.BusinessError;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public record GitHubOAuthProperties(boolean enabled, String clientId, String clientSecret,
                                    String redirectUri, String frontendRedirectUri) {
    public GitHubOAuthProperties(
            @Value("${app.auth.github.enabled:${APP_AUTH_GITHUB_ENABLED:false}}") boolean enabled,
            @Value("${app.auth.github.client-id:${APP_AUTH_GITHUB_CLIENT_ID:}}") String clientId,
            @Value("${app.auth.github.client-secret:${APP_AUTH_GITHUB_CLIENT_SECRET:}}") String clientSecret,
            @Value("${app.auth.github.redirect-uri:${APP_AUTH_GITHUB_REDIRECT_URI:}}") String redirectUri,
            @Value("${app.auth.github.frontend-redirect-uri:${APP_AUTH_GITHUB_FRONTEND_REDIRECT_URI:}}") String frontendRedirectUri) {
        this.enabled = enabled;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.frontendRedirectUri = frontendRedirectUri;
    }

    public void requireEnabled() {
        if (!enabled || clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()
                || !validHttpUri(redirectUri, false) || !validHttpUri(frontendRedirectUri, true)
                || !sameOrigin(redirectUri, frontendRedirectUri)) {
            throw new BizException(BusinessError.GITHUB_DISABLED);
        }
    }

    public boolean secureCookie() {
        return redirectUri != null && redirectUri.regionMatches(true, 0, "https://", 0, 8);
    }

    private static boolean sameOrigin(String callback, String frontend) {
        URI left = URI.create(callback);
        URI right = URI.create(frontend);
        return left.getScheme().equalsIgnoreCase(right.getScheme()) && left.getHost().equalsIgnoreCase(right.getHost())
                && effectivePort(left) == effectivePort(right);
    }

    private static int effectivePort(URI uri) {
        return uri.getPort() >= 0 ? uri.getPort() : "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    @Override
    public String toString() {
        return "GitHubOAuthProperties[enabled=" + enabled + "]";
    }

    private static boolean validHttpUri(String value, boolean allowFragment) {
        try {
            URI uri = URI.create(value);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getRawUserInfo() == null && uri.getRawQuery() == null
                    && (allowFragment || uri.getRawFragment() == null);
        } catch (IllegalArgumentException | NullPointerException exception) {
            return false;
        }
    }
}
