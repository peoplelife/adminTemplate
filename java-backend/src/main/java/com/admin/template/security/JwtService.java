package com.admin.template.security;

import com.admin.template.common.exception.BizException;
import com.admin.template.common.exception.BusinessError;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {
    private static final long TOKEN_LIFETIME_MILLIS = 24L * 60 * 60 * 1000;
    private static final String DEVELOPMENT_SECRET = "development-only-change-me-32-byte-secret";

    private final SecretKey signingKey;

    @Autowired
    public JwtService(@Value("${app.jwt.secret:}") String secret,
                      @Value("${spring.profiles.active:}") String activeProfiles) {
        this(secret, activeProfiles.split(","));
    }

    public JwtService(String secret, boolean production) {
        this(secret, production ? new String[]{"prod"} : new String[0]);
    }

    private JwtService(String secret, String[] activeProfiles) {
        boolean production = List.of(activeProfiles).stream().anyMatch("prod"::equalsIgnoreCase);
        String effectiveSecret = secret == null || secret.isBlank() ? DEVELOPMENT_SECRET : secret;
        if (production && (secret == null || secret.isBlank())) {
            throw new IllegalStateException("JWT_SECRET must be configured in production");
        }
        byte[] keyBytes = effectiveSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createToken(long id, String username, List<String> roles) {
        return createToken(id, username, roles, Instant.now());
    }

    public String createToken(long id, String username, List<String> roles, Instant issuedAtInstant) {
        Date issuedAt = Date.from(issuedAtInstant);
        return Jwts.builder()
                .claim("id", id)
                .claim("username", username)
                .claim("roles", roles == null ? List.of() : roles)
                .issuedAt(issuedAt)
                .expiration(new Date(issuedAt.getTime() + TOKEN_LIFETIME_MILLIS))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public AuthenticatedUser parseToken(String token) {
        return parseTokenDetails(token).user();
    }

    public TokenDetails parseTokenDetails(String token) {
        try {
            var parsedToken = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
            if (!"HS256".equals(parsedToken.getHeader().getAlgorithm())) {
                throw new IllegalArgumentException("Unexpected JWT signing algorithm");
            }
            Claims claims = parsedToken.getPayload();
            Number id = claims.get("id", Number.class);
            String username = claims.get("username", String.class);
            List<?> rawRoles = claims.get("roles", List.class);
            Date issuedAt = claims.getIssuedAt();
            if (id == null || username == null || rawRoles == null || issuedAt == null
                    || rawRoles.stream().anyMatch(role -> !(role instanceof String))) {
                throw new IllegalArgumentException("Missing identity claims");
            }
            return new TokenDetails(new AuthenticatedUser(id.longValue(), username,
                    rawRoles.stream().map(String.class::cast).toList()), issuedAt.toInstant());
        } catch (Exception exception) {
            throw new BizException(BusinessError.AUTH_TOKEN_INVALID);
        }
    }

    public record TokenDetails(AuthenticatedUser user, Instant issuedAt) {
    }
}
