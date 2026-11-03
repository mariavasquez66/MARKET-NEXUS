package application.infrastructure.security;

import application.domain.ports.out.PasswordServicePort;
import application.domain.valueobjects.PasswordHash;
import java.util.Objects;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordServiceAdapter implements PasswordServicePort {
    private final PasswordEncoder passwordEncoder;

    public PasswordServiceAdapter(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder cannot be null");
    }

    @Override
    public PasswordHash encode(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        return PasswordHash.of(passwordEncoder.encode(rawPassword));
    }

    @Override
    public boolean matches(String rawPassword, PasswordHash passwordHash) {
        if (rawPassword == null || rawPassword.isBlank() || passwordHash == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, passwordHash.getValue());
    }
}
