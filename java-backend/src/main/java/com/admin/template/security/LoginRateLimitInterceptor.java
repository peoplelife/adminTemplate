package com.admin.template.security;

import com.admin.template.common.exception.BizException;
import com.admin.template.common.exception.BusinessError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;

/** Applies login throttling before Spring MVC binds and validates the request body. */
@Component
public class LoginRateLimitInterceptor implements HandlerInterceptor {
    public static final String[] LOGIN_PATHS = {"/v1/auth/login", "/v1/auth/github/authorize",
            "/v1/auth/github/complete", "/v1/me/github/authorize", "/v1/me/github/complete"};
    private final LoginRateLimiter rateLimiter;

    public LoginRateLimitInterceptor(LoginRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!"POST".equalsIgnoreCase(request.getMethod()) || !java.util.Arrays.asList(LOGIN_PATHS).contains(path)) {
            return true;
        }

        String ip = request.getRemoteAddr();
        if (rateLimiter.tryAcquire(ip)) return true;

        response.setHeader("Retry-After",
                Long.toString(rateLimiter.retryAfterSeconds(ip, Instant.now())));
        throw new BizException(BusinessError.RATE_LIMITED);
    }
}
