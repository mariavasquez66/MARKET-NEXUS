package application.infrastructure.security;

import application.domain.enums.UserRole;
import java.time.Instant;
import java.util.Objects;

public record JwtTokenClaims(
        String subject,
        String tokenId,
        long tokenVersion,
        UserRole role,
        Instant issuedAt,
        Instant expiresAt) {

    public JwtTokenClaims {
        Objects.requireNonNull(subject, "subject cannot be null");
        Objects.requireNonNull(tokenId, "tokenId cannot be null");
        Objects.requireNonNull(role, "role cannot be null");
        Objects.requireNonNull(issuedAt, "issuedAt cannot be null");
        Objects.requireNonNull(expiresAt, "expiresAt cannot be null");
    }
}
