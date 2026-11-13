package com.admin.template.security;

import com.admin.template.common.exception.BusinessError;
import com.admin.template.common.response.Result;
import com.admin.template.common.web.TraceIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.admin.template.entity.UserEntity;
import com.admin.template.mapper.UserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.time.ZoneOffset;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    public static final String USER_ATTRIBUTE = AuthenticatedUser.class.getName();
    public static final RequestMatcher PUBLIC_ENDPOINTS = request -> {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return "/".equals(path) || "/v1/auth/login".equals(path)
                || "/v1/auth/github/config".equals(path) || "/v1/auth/github/authorize".equals(path)
                || "/v1/auth/github/callback".equals(path) || "/v1/auth/github/complete".equals(path)
                || under(path, "/docs") || under(path, "/swagger-ui")
                || under(path, "/v3/api-docs") || under(path, "/public");
    };
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Autowired
    public JwtAuthenticationFilter(JwtService jwtService, UserMapper userMapper, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return PUBLIC_ENDPOINTS.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.matches("(?i)^Bearer\\s+\\S+$")) {
            writeUnauthorized(request, response, BusinessError.AUTH_REQUIRED);
            return;
        }

        AuthenticatedUser user;
        JwtService.TokenDetails token;
        try {
            token = jwtService.parseTokenDetails(authorization.replaceFirst("(?i)^Bearer\\s+", ""));
        } catch (RuntimeException exception) {
            writeUnauthorized(request, response, BusinessError.AUTH_TOKEN_INVALID);
            return;
        }
        UserEntity account = userMapper.selectById(token.user().id());
        if (account == null || account.deletedAt != null
                || account.lastLogout != null && !token.issuedAt().isAfter(account.lastLogout.toInstant(ZoneOffset.UTC))) {
            writeUnauthorized(request, response, BusinessError.AUTH_TOKEN_INVALID);
            return;
        }
        if (!Boolean.TRUE.equals(account.status)) {
            writeUnauthorized(request, response, BusinessError.ACCOUNT_DISABLED);
            return;
        }
        user = token.user();
        request.setAttribute(USER_ATTRIBUTE, user);
        var authorities = user.roles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, authorities));
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static boolean under(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response, BusinessError error)
            throws IOException {
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        Object traceId = request.getAttribute(TraceIdFilter.ATTRIBUTE_NAME);
        LOGGER.warn("Authentication failed httpStatus=200 resultCode={} reason={} method={} path={} traceId={}",
                error.code(), error.name(), request.getMethod(), request.getRequestURI(), traceId);
        Result<?> result = Result.fail(error.code(), error.message(), null,
                traceId == null ? "" : traceId.toString());
        objectMapper.writeValue(response.getOutputStream(), result);
    }
}
