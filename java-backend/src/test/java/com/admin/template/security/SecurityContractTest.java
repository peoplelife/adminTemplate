package com.admin.template.security;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.admin.template.common.exception.BizException;
import com.admin.template.common.exception.BusinessError;
import com.admin.template.common.web.TraceIdFilter;
import com.admin.template.mapper.MenuMapper;
import com.admin.template.mapper.UserMapper;
import com.admin.template.mapper.UserRoleMapper;
import com.admin.template.entity.UserEntity;
import com.admin.template.dto.security.ActiveRole;
import com.admin.template.dto.security.MenuPermission;
import com.admin.template.security.AuthenticatedUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityContractTest {
    private static final String SECRET = "01234567890123456789012345678901";

    @Test
    void createsHs256TokenWithIdentityRolesAndTwentyFourHourLifetime() {
        JwtService jwtService = new JwtService(SECRET, false);

        String token = jwtService.createToken(42L, "alice", List.of("operator", "auditor"));
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(Jwts.parser().verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build().parseSignedClaims(token).getHeader().getAlgorithm()).isEqualTo("HS256");
        assertThat(claims.get("id", Long.class)).isEqualTo(42L);
        assertThat(claims.get("username", String.class)).isEqualTo("alice");
        assertThat(claims.get("roles", List.class)).containsExactly("operator", "auditor");
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
                .isEqualTo(Duration.ofHours(24).toMillis());
        assertThat(jwtService.parseToken(token))
                .isEqualTo(new AuthenticatedUser(42L, "alice", List.of("operator", "auditor")));
    }

    @Test
    void rejectsTokensSignedWithAlgorithmsOtherThanHs256() {
        String longSecret = "0123456789012345678901234567890123456789012345678901234567890123";
        byte[] strongerKey = longSecret.getBytes(StandardCharsets.UTF_8);
        Date now = new Date();
        String token = Jwts.builder().claim("id", 42L).claim("username", "alice").claim("roles", List.of("operator"))
                .issuedAt(now).expiration(new Date(now.getTime() + Duration.ofHours(24).toMillis()))
                .signWith(Keys.hmacShaKeyFor(strongerKey), Jwts.SIG.HS512).compact();
        JwtService jwtService = new JwtService(longSecret, false);

        assertThatThrownBy(() -> jwtService.parseToken(token))
                .isInstanceOf(BizException.class).extracting(error -> ((BizException) error).error())
                .isEqualTo(BusinessError.AUTH_TOKEN_INVALID);
    }

    @Test
    void rejectsMissingAndInvalidBearerTokensBeforeCallingApplication() throws Exception {
        JwtService jwtService = new JwtService(SECRET, false);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, mock(UserMapper.class), new ObjectMapper().findAndRegisterModules());
        AtomicInteger calls = new AtomicInteger();
        FilterChain chain = (request, response) -> calls.incrementAndGet();

        MockHttpServletResponse missingResponse = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/v1/users"), missingResponse, chain);
        MockHttpServletRequest invalidRequest = new MockHttpServletRequest("GET", "/v1/users");
        invalidRequest.addHeader("Authorization", "Bearer not-a-jwt");
        MockHttpServletResponse invalidResponse = new MockHttpServletResponse();
        filter.doFilter(invalidRequest, invalidResponse, chain);

        assertThat(missingResponse.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(invalidResponse.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(missingResponse.getContentAsString()).contains("\"code\":110005");
        assertThat(invalidResponse.getContentAsString()).contains("\"code\":110002");
        assertThat(calls).hasValue(0);
    }

    @Test
    void rejectsDisabledAndLoggedOutUsersBeforeCallingApplication() throws Exception {
        UserMapper users = mock(UserMapper.class);
        UserEntity disabled = new UserEntity();
        disabled.id = 42L;
        disabled.status = false;
        when(users.selectById(42L)).thenReturn(disabled);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(new JwtService(SECRET, false), users, new ObjectMapper().findAndRegisterModules());
        String token = new JwtService(SECRET, false).createToken(42L, "alice", List.of("operator"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/v1/users") {{
            addHeader("Authorization", "Bearer " + token);
        }}, response, (request, output) -> { throw new AssertionError("disabled user reached the application"); });

        assertThat(response.getContentAsString()).contains("\"code\":110003");
    }

    @Test
    void rejectsRevokedMissingAndDeletedAccountsAndAcceptsNewerToken() throws Exception {
        UserMapper users = mock(UserMapper.class);
        JwtService jwt = new JwtService(SECRET, false);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwt, users, new ObjectMapper().findAndRegisterModules());
        UserEntity account = new UserEntity();
        account.id = 42L;
        account.status = true;
        Instant cutoff = Instant.now().minusSeconds(10).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        account.lastLogout = java.time.LocalDateTime.ofInstant(cutoff, java.time.ZoneOffset.UTC);
        when(users.selectById(42L)).thenReturn(account);
        AtomicInteger calls = new AtomicInteger();
        for (Instant issuedAt : List.of(cutoff.minusSeconds(1), cutoff, cutoff.plusSeconds(1))) {
            var request = new MockHttpServletRequest("GET", "/v1/new-module");
            request.addHeader("Authorization", "Bearer " + jwt.createToken(42L, "alice", List.of(), issuedAt));
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, (input, output) -> calls.incrementAndGet());
            if (!issuedAt.isAfter(cutoff)) assertThat(response.getContentAsString()).contains("\"code\":110002");
        }
        assertThat(calls).hasValue(1);
        for (boolean deleted : List.of(false, true)) {
            account.deletedAt = java.time.LocalDateTime.now();
            when(users.selectById(42L)).thenReturn(deleted ? account : null);
            var request = new MockHttpServletRequest("GET", "/v1/new-module");
            request.addHeader("Authorization", "Bearer " + jwt.createToken(42L, "alice", List.of()));
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, (input, output) -> calls.incrementAndGet());
            assertThat(response.getContentAsString()).contains("\"code\":110002");
        }
        assertThat(calls).hasValue(1);
    }

    @Test
    void logsRejectedAuthenticationWithoutExposingToken() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(JwtAuthenticationFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        appender.start();
        logger.addAppender(appender);

        String token = "not-a-jwt";
        try {
            JwtAuthenticationFilter filter = new JwtAuthenticationFilter(new JwtService(SECRET, false), mock(UserMapper.class), new ObjectMapper().findAndRegisterModules());
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/users");
            request.setAttribute(TraceIdFilter.ATTRIBUTE_NAME, "request-123");
            request.addHeader("Authorization", "Bearer " + token);
            filter.doFilter(request, new MockHttpServletResponse(), (request1, response1) -> {
            });

            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage()).contains("AUTH_TOKEN_INVALID", "GET", "/v1/users", "request-123");
            });
            assertThat(appender.list).noneMatch(event -> event.getFormattedMessage().contains(token));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void allowsSuperAdminThroughSpecificPermissionCheck() {
        UserRoleMapper userRoleMapper = mock(UserRoleMapper.class);
        MenuMapper menuMapper = mock(MenuMapper.class);
        when(userRoleMapper.selectActiveRolesByUserId(7L))
                .thenReturn(List.of(new ActiveRole(3L, "super_admin", "Super Admin")));
        when(menuMapper.selectActivePermissionsForAllMenus())
                .thenReturn(List.of(new MenuPermission("menu_home", null)));
        PermissionService permissions = new PermissionService(userRoleMapper, menuMapper);

        permissions.requirePermissions(new AuthenticatedUser(7L, "root", List.of("super_admin")), "user:create");
    }

    @Test
    void reportsSeparateErrorsForNoActiveRoleAndNoAssignedMenu() {
        UserRoleMapper userRoleMapper = mock(UserRoleMapper.class);
        MenuMapper menuMapper = mock(MenuMapper.class);
        when(userRoleMapper.selectActiveRolesByUserId(8L)).thenReturn(List.of());
        when(userRoleMapper.selectActiveRolesByUserId(9L))
                .thenReturn(List.of(new ActiveRole(5L, "operator", "Operator")));
        when(menuMapper.selectActivePermissionsForRoleIds(List.of(5L))).thenReturn(List.of());
        PermissionService permissions = new PermissionService(userRoleMapper, menuMapper);

        assertThatThrownBy(() -> permissions.requirePermissions(
                new AuthenticatedUser(8L, "norole", List.of()), "user:create"))
                .isInstanceOf(BizException.class).extracting(error -> ((BizException) error).error())
                .isEqualTo(BusinessError.ACCOUNT_WITHOUT_ROLE);
        assertThatThrownBy(() -> permissions.requirePermissions(
                new AuthenticatedUser(9L, "nomenu", List.of("operator")), "user:create"))
                .isInstanceOf(BizException.class).extracting(error -> ((BizException) error).error())
                .isEqualTo(BusinessError.ACCOUNT_WITHOUT_PERMISSION);
    }

    @Test
    void limitsEachIpToTenRequestsInAOneMinuteWindow() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        Instant start = Instant.parse("2026-10-22T04:00:00Z");

        for (int attempt = 0; attempt < 10; attempt++) {
            assertThat(limiter.tryAcquire("203.0.113.4", start)).isTrue();
        }
        assertThat(limiter.tryAcquire("203.0.113.4", start)).isFalse();
        assertThat(limiter.tryAcquire("203.0.113.5", start)).isTrue();
        assertThat(limiter.tryAcquire("203.0.113.4", start.plusSeconds(60))).isTrue();
    }
}
