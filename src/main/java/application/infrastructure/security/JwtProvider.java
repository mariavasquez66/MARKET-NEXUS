package application.infrastructure.security;

import application.domain.enums.UserRole;
import application.domain.exceptions.common.InvalidDataException;
import application.domain.models.User;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.JwtServicePort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider implements JwtServicePort {
    private static final int MINIMUM_SECRET_BYTES = 32;

    private final SecretKey signingKey;
    private final long expirationMs;
    private final ClockPort clockPort;

    public JwtProvider(@Value("${jwt.secret:}") String secret,
                       @Value("${jwt.expiration-ms:3600000}") long expirationMs,
                       ClockPort clockPort) {
        byte[] secretBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET must contain at least " + MINIMUM_SECRET_BYTES + " bytes");
        }
        if (expirationMs <= 0) {
            throw new IllegalStateException("JWT_EXPIRATION_MS must be greater than zero");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.expirationMs = expirationMs;
        this.clockPort = Objects.requireNonNull(clockPort, "clockPort cannot be null");
    }

    @Override
    public String issueToken(User user) {
        Objects.requireNonNull(user, "user cannot be null");
        Instant issuedAt = clockPort.now();
        Instant expiresAt = issuedAt.plusMillis(expirationMs);
        return Jwts.builder()
                .subject(user.getId())
                .id(UUID.randomUUID().toString())
                .claim("ver", user.getAuthTokenVersion())
                .claim("role", user.getRole().getCode())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public JwtTokenClaims validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtException("Token is required");
        }
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .clock(() -> Date.from(clockPort.now()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new JwtException("Invalid or expired token", ex);
        }

        String subject = claims.getSubject();
        String tokenId = claims.getId();
        Object versionClaim = claims.get("ver");
        String roleClaim = claims.get("role", String.class);
        Date issuedAt = claims.getIssuedAt();
        Date expiresAt = claims.getExpiration();
        if (subject == null || subject.isBlank()
                || tokenId == null || tokenId.isBlank()
                || !(versionClaim instanceof Number version)
                || roleClaim == null || roleClaim.isBlank()
                || issuedAt == null || expiresAt == null) {
            throw new JwtException("Token is missing required claims");
        }
        UserRole role;
        try {
            role = UserRole.fromCode(roleClaim);
        } catch (InvalidDataException ex) {
            throw new JwtException("Token contains an invalid role", ex);
        }
        return new JwtTokenClaims(subject, tokenId, version.longValue(), role,
                issuedAt.toInstant(), expiresAt.toInstant());
    }
}
