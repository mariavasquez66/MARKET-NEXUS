package application.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import application.domain.enums.UserRole;
import application.domain.models.User;
import application.domain.ports.out.ClockPort;
import application.domain.valueobjects.Email;
import application.domain.valueobjects.FullName;
import application.domain.valueobjects.IdentityDocument;
import application.domain.valueobjects.PasswordHash;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtProviderTest {
    private static final String SECRET = "test-secret-for-jwt-provider-with-enough-bytes-0123456789";
    private static final String OTHER_SECRET = "another-test-secret-with-enough-bytes-9876543210xyz";
    private static final long EXPIRATION_MS = 3_600_000L;

    private MutableClock clock;
    private JwtProvider provider;
    private User user;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
        provider = new JwtProvider(SECRET, EXPIRATION_MS, clock);
        user = User.reconstitute(
                "user-1",
                FullName.of("Jane Buyer"),
                Email.of("jane@nexusmarket.test"),
                IdentityDocument.of("DOC12345"),
                UserRole.BUYER,
                application.domain.enums.UserStatus.ACTIVE,
                PasswordHash.of("$2a$10$stored-hash"),
                7L);
    }

    @Test
    void issuesTokenWithRequiredInformativeClaimsAndNoPii() {
        String token = provider.issueToken(user);
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Claims raw = Jwts.parser().verifyWith(key)
                .clock(() -> Date.from(clock.now()))
                .build()
                .parseSignedClaims(token).getPayload();

        assertThat(raw.getSubject()).isEqualTo("user-1");
        assertThat(raw.getId()).isNotBlank();
        assertThat(((Number) raw.get("ver")).longValue()).isEqualTo(7L);
        assertThat(raw.get("role", String.class)).isEqualTo("BUYER");
        assertThat(raw.getIssuedAt().toInstant()).isEqualTo(clock.now());
        assertThat(raw.getExpiration().toInstant()).isEqualTo(clock.now().plusMillis(EXPIRATION_MS));
        assertThat(raw.keySet()).containsExactlyInAnyOrder(
                "sub", "jti", "ver", "iat", "exp", "role");
        assertThat(token).doesNotContain("jane@nexusmarket.test", "Jane Buyer", "DOC12345");
    }

    @Test
    void validatesIssuedToken() {
        JwtTokenClaims claims = provider.validateToken(provider.issueToken(user));

        assertThat(claims.subject()).isEqualTo("user-1");
        assertThat(claims.tokenId()).isNotBlank();
        assertThat(claims.tokenVersion()).isEqualTo(7L);
        assertThat(claims.role()).isEqualTo(UserRole.BUYER);
        assertThat(claims.issuedAt()).isEqualTo(clock.now());
        assertThat(claims.expiresAt()).isEqualTo(clock.now().plusMillis(EXPIRATION_MS));
    }

    @Test
    void rejectsExpiredToken() {
        String token = provider.issueToken(user);
        clock.advanceMillis(EXPIRATION_MS + 1);

        assertThatThrownBy(() -> provider.validateToken(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        JwtProvider other = new JwtProvider(OTHER_SECRET, EXPIRATION_MS, clock);
        String token = other.issueToken(user);

        assertThatThrownBy(() -> provider.validateToken(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsMalformedOrBlankToken() {
        assertThatThrownBy(() -> provider.validateToken("not-a-jwt"))
                .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> provider.validateToken(" "))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void failsFastWhenSecretIsTooShortOrExpirationIsInvalid() {
        assertThatThrownBy(() -> new JwtProvider("short", EXPIRATION_MS, clock))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
        assertThatThrownBy(() -> new JwtProvider(SECRET, 0, clock))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_EXPIRATION_MS");
    }

    private static final class MutableClock implements ClockPort {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        void advanceMillis(long millis) {
            now = now.plusMillis(millis);
        }

        @Override
        public Instant now() {
            return now;
        }

        @Override
        public LocalDate today() {
            return LocalDate.ofInstant(now, ZoneOffset.UTC);
        }
    }
}
