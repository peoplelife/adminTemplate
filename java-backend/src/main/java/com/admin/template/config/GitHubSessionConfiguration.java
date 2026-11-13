package com.admin.template.config;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GitHubSessionConfiguration {
    @Bean
    ServletContextInitializer githubSessionCookies(GitHubOAuthProperties properties) {
        return context -> {
            var cookie = context.getSessionCookieConfig();
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setAttribute("SameSite", "Lax");
            // The registered callback scheme remains reliable behind a TLS terminating proxy.
            if (properties.secureCookie()) cookie.setSecure(true);
            context.setSessionTrackingModes(java.util.Set.of(jakarta.servlet.SessionTrackingMode.COOKIE));
        };
    }
}
