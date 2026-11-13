package com.admin.template.service.auth;

import com.admin.template.common.exception.AppException;
import com.admin.template.common.exception.BizException;
import com.admin.template.common.exception.BusinessError;
import com.admin.template.common.exception.SystemError;
import com.admin.template.config.GitHubOAuthProperties;
import com.admin.template.dto.auth.LoginResponse;
import com.admin.template.entity.UserOAuthAccountEntity;
import com.admin.template.mapper.UserOAuthAccountMapper;
import com.admin.template.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GitHubOAuthServiceTest {
    private final GitHubOAuthClient client = mock(GitHubOAuthClient.class);
    private final UserOAuthAccountMapper accounts = mock(UserOAuthAccountMapper.class);
    private final AuthService auth = mock(AuthService.class);
    private final GitHubRegistrationService registration = mock(GitHubRegistrationService.class);
    private final UserMapper users = mock(UserMapper.class);
    private final Clock clock = mock(Clock.class);
    private final MockHttpSession session = new MockHttpSession();
    private final Instant now = Instant.parse("2026-11-13T08:00:00Z");
    private GitHubOAuthService service;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(now);
        service = new GitHubOAuthService(properties(), client, accounts, auth, registration, users, clock);
        when(client.fetchProfile(any(), any())).thenReturn(new GitHubOAuthClient.Profile("42", "octocat"));
    }

    static GitHubOAuthProperties properties() {
        return new GitHubOAuthProperties(true, "client", "secret", "https://frontend.example/api/v1/auth/github/callback",
                "https://frontend.example/#/oauth/github");
    }

    @Test
    void keepsPkceVerifierInTheSessionAndSendsItsS256ChallengeOnly() throws Exception {
        var start = service.authorize(session, null);
        var query = UriComponentsBuilder.fromUriString(start.authorizationUrl()).build().getQueryParams();
        assertThat(query.getFirst("state")).isEqualTo(start.state());
        assertThat(start.state()).hasSizeGreaterThanOrEqualTo(43);
        assertThat(query.getFirst("code_challenge_method")).isEqualTo("S256");
        assertThat(query.getFirst("scope")).isNull();
        assertThat(start.authorizationUrl()).doesNotContain("secret", "code_verifier", "email", "repo");
        service.callback(session, "code", start.state(), null);
        var verifier = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(client).fetchProfile(eq("code"), verifier.capture());
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256")
                .digest(verifier.getValue().getBytes(StandardCharsets.US_ASCII)));
        assertThat(query.getFirst("code_challenge")).isEqualTo(challenge);
        assertThat(session.getMaxInactiveInterval()).isEqualTo(300);
    }

    @Test
    void ignoresMismatchedStateWithoutOverwritingTheValidFlowAndRejectsReplay() {
        var start = service.authorize(session, null);
        service.callback(session, "attacker-code", "wrong-state", null);
        service.callback(session, "code", start.state(), null);
        service.callback(session, "replay-code", start.state(), null);
        when(accounts.selectOne(any(QueryWrapper.class))).thenReturn(account(7, "42"));
        when(auth.loginTrustedUser(7)).thenReturn(new LoginResponse("jwt", false));
        assertThat(service.completeLogin(session, start.state()).token()).isEqualTo("jwt");
        assertThatThrownBy(() -> service.completeLogin(session, start.state())).isInstanceOf(BizException.class);
        verify(client, times(1)).fetchProfile(any(), any());
    }

    @Test
    void requiresTheSameSessionAndAnUnexpiredCompletedResult() {
        var start = service.authorize(session, null);
        assertThatThrownBy(() -> service.completeLogin(new MockHttpSession(), start.state())).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> service.completeLogin(session, start.state())).isInstanceOf(BizException.class);
        when(clock.instant()).thenReturn(now.plusSeconds(301));
        service.callback(session, "code", start.state(), null);
        assertThatThrownBy(() -> service.completeLogin(session, start.state())).isInstanceOf(BizException.class);
        verifyNoInteractions(client);
    }

    @Test
    void expiresResultsEvenWhenTheProviderAlreadyReturned() {
        var start = service.authorize(session, null);
        service.callback(session, "code", start.state(), null);
        when(clock.instant()).thenReturn(now.plusSeconds(300));
        assertThatThrownBy(() -> service.completeLogin(session, start.state())).isInstanceOf(BizException.class);
        verifyNoInteractions(accounts, auth);
    }

    @Test
    void denialAndTransportErrorsBecomeSafeSingleUseResults() {
        var denied = service.authorize(session, null);
        service.callback(session, null, denied.state(), "access_denied secret-from-query");
        assertThatThrownBy(() -> service.completeLogin(session, denied.state())).isInstanceOf(BizException.class)
                .hasMessage(BusinessError.GITHUB_DENIED.message());
        verifyNoInteractions(client);
        var failed = service.authorize(session, null);
        when(client.fetchProfile(any(), any())).thenThrow(new AppException(SystemError.GITHUB_SERVICE_ERROR, null, null));
        service.callback(session, "private-code", failed.state(), null);
        assertThatThrownBy(() -> service.completeLogin(session, failed.state())).isInstanceOf(AppException.class)
                .hasMessageNotContaining("private-code").hasMessageNotContaining("secret").hasCause(null);
        assertThatThrownBy(() -> service.completeLogin(session, failed.state())).isInstanceOf(BizException.class);
    }

    @Test
    void unboundGitHubRegistersWithoutMergingAccounts() {
        var start = service.authorize(session, null);
        service.callback(session, "code", start.state(), null);
        when(registration.register(new GitHubOAuthClient.Profile("42", "octocat"))).thenReturn(new LoginResponse("new-jwt", true));
        assertThat(service.completeLogin(session, start.state()).token()).isEqualTo("new-jwt");
        verifyNoInteractions(auth);
    }

    @Test
    void afterRegistrationRaceRollbackLogsInTheExistingWinner() {
        when(accounts.selectOne(any(QueryWrapper.class))).thenReturn(null, account(9, "42"));
        when(registration.register(any())).thenThrow(new DuplicateKeyException("private SQL"));
        when(auth.loginTrustedUser(9)).thenReturn(new LoginResponse("winner-jwt", false));
        var start = service.authorize(session, null);
        service.callback(session, "code", start.state(), null);
        assertThat(service.completeLogin(session, start.state()).token()).isEqualTo("winner-jwt");
    }

    @Test
    void doesNotConvertAnUnrelatedUniqueFailureToASuccessfulLogin() {
        var failure = new DuplicateKeyException("private SQL");
        when(registration.register(any())).thenThrow(failure);
        var start = service.authorize(session, null);
        service.callback(session, "code", start.state(), null);
        assertThatThrownBy(() -> service.completeLogin(session, start.state())).isSameAs(failure);
        verifyNoInteractions(auth);
    }

    @Test
    void validatesBindingModeAndOwnerWithoutConsumingAnotherUsersResult() {
        var start = service.authorize(session, 7L);
        service.callback(session, "code", start.state(), null);
        assertThatThrownBy(() -> service.completeLogin(session, start.state())).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> service.completeBinding(session, start.state(), 8L)).isInstanceOf(BizException.class);
        var result = service.completeBinding(session, start.state(), 7L);
        assertThat(result.bound()).isTrue();
        assertThat(result.login()).isEqualTo("octocat");
        verify(accounts).insert(any(UserOAuthAccountEntity.class));
        assertThatThrownBy(() -> service.completeBinding(session, start.state(), 7L)).isInstanceOf(BizException.class);
    }

    @Test
    void neverTakesOverAnExistingProviderOrLocalBindingAndTranslatesRaces() {
        when(accounts.selectOne(any(QueryWrapper.class))).thenReturn(account(8, "42"));
        var start = service.authorize(session, 7L);
        service.callback(session, "code", start.state(), null);
        assertThatThrownBy(() -> service.completeBinding(session, start.state(), 7L)).isInstanceOf(BizException.class)
                .hasMessage(BusinessError.GITHUB_BINDING_CONFLICT.message());
        verify(accounts, never()).insert(any(UserOAuthAccountEntity.class));

        when(accounts.selectOne(any(QueryWrapper.class))).thenReturn(null);
        when(accounts.insert(any(UserOAuthAccountEntity.class))).thenThrow(new DuplicateKeyException("private SQL"));
        var race = service.authorize(session, 7L);
        service.callback(session, "code", race.state(), null);
        assertThatThrownBy(() -> service.completeBinding(session, race.state(), 7L)).isInstanceOf(BizException.class)
                .hasMessage(BusinessError.GITHUB_BINDING_CONFLICT.message()).hasCause(null);
    }

    @Test
    void disabledBindingViewWorksBeforeTheOptionalTableIsMigrated() {
        var disabled = new GitHubOAuthService(new GitHubOAuthProperties(false, "", "", "", ""), client, accounts,
                auth, registration, users, clock);
        var binding = disabled.binding(7L);
        assertThat(binding.enabled()).isFalse();
        assertThat(binding.bound()).isFalse();
        assertThat(binding.login()).isNull();
        verifyNoInteractions(accounts);
    }

    private static UserOAuthAccountEntity account(long userId, String providerId) {
        var account = new UserOAuthAccountEntity();
        account.id = 1L;
        account.userId = userId;
        account.provider = "github";
        account.providerUserId = providerId;
        account.providerUsername = "octocat";
        return account;
    }
}
