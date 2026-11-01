package application.domain.models;

import application.domain.exceptions.common.InvalidDataException;
import java.util.Objects;

public record AuthResult(String token, User user) {
    public AuthResult {
        if (token == null || token.isBlank()) {
            throw new InvalidDataException("Authentication token is required");
        }
        Objects.requireNonNull(user, "user cannot be null");
    }
}
