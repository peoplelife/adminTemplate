package com.admin.template.service.auth;

import com.admin.template.common.exception.AppException;
import com.admin.template.common.exception.BizException;
import com.admin.template.common.exception.BusinessError;
import com.admin.template.common.exception.SystemError;
import com.admin.template.config.GitHubOAuthProperties;
import com.admin.template.dto.auth.GitHubOAuthDtos.Authorization;
import com.admin.template.dto.auth.GitHubOAuthDtos.Binding;
import com.admin.template.dto.auth.LoginResponse;
import com.admin.template.entity.UserOAuthAccountEntity;
import com.admin.template.mapper.UserOAuthAccountMapper;
import com.admin.template.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Objects;

@Service
public class GitHubOAuthService {
    private static final String SESSION_KEY = GitHubOAuthService.class.getName();
    private static final String PROVIDER = "github";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final GitHubOAuthProperties properties;
    private final GitHubOAuthClient client;
    private final UserOAuthAccountMapper accounts;
    private final AuthService auth;
    private final GitHubRegistrationService registration;
    private final UserMapper users;
    private final Clock clock;

    @Autowired
    public GitHubOAuthService(GitHubOAuthProperties properties, GitHubOAuthClient client,
                              UserOAuthAccountMapper accounts, AuthService auth,
                              GitHubRegistrationService registration, UserMapper users) {
        this(properties, client, accounts, auth, registration, users, Clock.systemUTC());
    }

    GitHubOAuthService(GitHubOAuthProperties properties, GitHubOAuthClient client,
                       UserOAuthAccountMapper accounts, AuthService auth,
                       GitHubRegistrationService registration, UserMapper users, Clock clock) {
        this.properties = properties;
        this.client = client;
        this.accounts = accounts;
        this.auth = auth;
        this.registration = registration;
        this.users = users;
        this.clock = clock;
    }

    public Authorization authorize(HttpSession session, Long owner) {
        properties.requireEnabled();
        String state = randomValue();
        String verifier = randomValue();
        synchronized (session) {
            session.setMaxInactiveInterval(300);
            session.setAttribute(SESSION_KEY, new Flow(state, owner, clock.instant().plusSeconds(300), verifier,
                    false, null, null));
        }
        String url = UriComponentsBuilder.fromUriString("https://github.com/login/oauth/authorize")
                .queryParam("client_id", properties.clientId()).queryParam("redirect_uri", properties.redirectUri())
                .queryParam("state", state).queryParam("code_challenge", challenge(verifier))
                .queryParam("code_challenge_method", "S256").build().encode().toUriString();
        return new Authorization(url, state);
    }

    public void callback(HttpSession session, String code, String state, String error) {
        if (session == null) return;
        Flow running;
        String verifier;
        synchronized (session) {
            Flow flow = (Flow) session.getAttribute(SESSION_KEY);
            if (flow == null || !flow.state().equals(state) || flow.verifier() == null) return;
            if (!clock.instant().isBefore(flow.expiresAt())) {
                session.removeAttribute(SESSION_KEY);
                return;
            }
            running = new Flow(flow.state(), flow.owner(), flow.expiresAt(), null, false, null, null);
            // Consume callback state before contacting GitHub, including simultaneous callbacks.
            session.setAttribute(SESSION_KEY, running);
            verifier = flow.verifier();
        }
        GitHubOAuthClient.Profile profile = null;
        Failure failure = null;
        try {
            if (error != null) failure = Failure.DENIED;
            else if (code == null || code.isBlank() || code.length() > 2048) failure = Failure.INVALID;
            else {
                properties.requireEnabled();
                profile = client.fetchProfile(code, verifier);
            }
        } catch (AppException | BizException exception) {
            failure = Failure.PROVIDER;
        }
        synchronized (session) {
            // A new authorization replaces the old flow; a late response cannot overwrite it.
            if (session.getAttribute(SESSION_KEY) == running) {
                session.setAttribute(SESSION_KEY, new Flow(running.state(), running.owner(), running.expiresAt(),
                        null, true, profile, failure));
            }
        }
    }

    public LoginResponse completeLogin(HttpSession session, String state) {
        properties.requireEnabled();
        var profile = consume(session, state, null);
        var account = findProviderBinding(profile.id());
        if (account != null) return auth.loginTrustedUser(account.userId);
        try {
            return registration.register(profile);
        } catch (DuplicateKeyException exception) {
            // The registration proxy has rolled back before reading the concurrent winner.
            account = findProviderBinding(profile.id());
            if (account == null) throw exception;
            return auth.loginTrustedUser(account.userId);
        }
    }

    @Transactional
    public Binding completeBinding(HttpSession session, String state, long userId) {
        properties.requireEnabled();
        var profile = consume(session, state, userId);
        UserOAuthAccountEntity providerAccount = accounts.selectOne(new QueryWrapper<UserOAuthAccountEntity>()
                .eq("provider", PROVIDER).eq("provider_user_id", profile.id()));
        UserOAuthAccountEntity localAccount = findBinding(userId);
        if (providerAccount != null && providerAccount.userId != userId
                || localAccount != null && !profile.id().equals(localAccount.providerUserId)) {
            throw new BizException(BusinessError.GITHUB_BINDING_CONFLICT);
        }
        if (localAccount == null) {
            var account = new UserOAuthAccountEntity();
            account.userId = userId;
            account.provider = PROVIDER;
            account.providerUserId = profile.id();
            account.providerUsername = profile.login();
            account.createdAt = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
            account.updatedAt = account.createdAt;
            try {
                accounts.insert(account);
            } catch (DuplicateKeyException exception) {
                throw new BizException(BusinessError.GITHUB_BINDING_CONFLICT);
            }
        }
        return new Binding(properties.enabled(), true, localAccount == null ? profile.login() : localAccount.providerUsername);
    }

    public Binding binding(long userId) {
        if (!properties.enabled()) return new Binding(false, false, null);
        var account = findBinding(userId);
        return new Binding(properties.enabled(), account != null, account == null ? null : account.providerUsername);
    }

    @Transactional
    public void unbind(long userId) {
        var user = users.selectById(userId);
        if (user == null) throw new BizException(BusinessError.USER_NOT_FOUND);
        if (user.password == null || user.password.isBlank()) {
            throw new BizException(BusinessError.GITHUB_ONLY_LOGIN);
        }
        accounts.delete(new QueryWrapper<UserOAuthAccountEntity>().eq("provider", PROVIDER).eq("user_id", userId));
    }

    private UserOAuthAccountEntity findProviderBinding(String providerId) {
        return accounts.selectOne(new QueryWrapper<UserOAuthAccountEntity>()
                .eq("provider", PROVIDER).eq("provider_user_id", providerId));
    }

    private UserOAuthAccountEntity findBinding(long userId) {
        return accounts.selectOne(new QueryWrapper<UserOAuthAccountEntity>().eq("provider", PROVIDER).eq("user_id", userId));
    }

    private GitHubOAuthClient.Profile consume(HttpSession session, String state, Long owner) {
        if (session == null) throw new BizException(BusinessError.GITHUB_FLOW_INVALID);
        Flow flow;
        synchronized (session) {
            flow = (Flow) session.getAttribute(SESSION_KEY);
            if (flow == null || !flow.state().equals(state) || !Objects.equals(owner, flow.owner())) {
                throw new BizException(BusinessError.GITHUB_FLOW_INVALID);
            }
            if (!clock.instant().isBefore(flow.expiresAt())) {
                session.removeAttribute(SESSION_KEY);
                throw new BizException(BusinessError.GITHUB_FLOW_INVALID);
            }
            if (!flow.complete()) throw new BizException(BusinessError.GITHUB_FLOW_INVALID);
            session.removeAttribute(SESSION_KEY);
        }
        if (flow.failure() == Failure.DENIED) throw new BizException(BusinessError.GITHUB_DENIED);
        if (flow.failure() == Failure.PROVIDER) throw new AppException(SystemError.GITHUB_SERVICE_ERROR, null, null);
        if (flow.failure() != null || flow.profile() == null) throw new BizException(BusinessError.GITHUB_FLOW_INVALID);
        return flow.profile();
    }

    private static String randomValue() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String challenge(String verifier) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256")
                    .digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable");
        }
    }

    private enum Failure { DENIED, INVALID, PROVIDER }
    private record Flow(String state, Long owner, Instant expiresAt, String verifier,
                        boolean complete, GitHubOAuthClient.Profile profile, Failure failure) {}
}
