package application.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import application.domain.valueobjects.PasswordHash;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordServiceAdapterTest {

    private final PasswordServiceAdapter adapter =
            new PasswordServiceAdapter(new BCryptPasswordEncoder(4));

    @Test
    void encodesWithBcryptAndMatchesRawPassword() {
        PasswordHash hash = adapter.encode("secret-password");

        assertThat(hash.getValue()).startsWith("$2");
        assertThat(adapter.matches("secret-password", hash)).isTrue();
        assertThat(adapter.matches("wrong-password", hash)).isFalse();
    }

    @Test
    void doesNotMatchMissingValues() {
        assertThat(adapter.matches(null, PasswordHash.of("$2a$04$hash"))).isFalse();
        assertThat(adapter.matches("secret", null)).isFalse();
    }
}
