package application.domain.ports.out;

import application.domain.valueobjects.PasswordHash;

public interface PasswordServicePort {
    PasswordHash encode(String rawPassword);

    boolean matches(String rawPassword, PasswordHash passwordHash);
}
