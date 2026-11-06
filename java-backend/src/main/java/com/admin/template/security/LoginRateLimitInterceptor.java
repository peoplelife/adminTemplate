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
    private static final String LOGIN_PATH = "/v1/auth/login";
    private final LoginRateLimiter rateLimiter;

    public LoginRateLimitInterceptor(LoginRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"POST".equalsIgnoreCase(request.getMethod()) || !LOGIN_PATH.equals(request.getRequestURI())) {
            return true;
        }

        String ip = request.getRemoteAddr();
        if (rateLimiter.tryAcquire(ip)) return true;

        response.setHeader("Retry-After",
                Long.toString(rateLimiter.retryAfterSeconds(ip, Instant.now())));
        throw new BizException(BusinessError.RATE_LIMITED);
    }
}
