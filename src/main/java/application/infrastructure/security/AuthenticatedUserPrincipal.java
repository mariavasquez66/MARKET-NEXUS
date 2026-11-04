package application.infrastructure.security;

import application.domain.models.User;
import java.util.Objects;

public final class AuthenticatedUserPrincipal {
    private final User user;

    public AuthenticatedUserPrincipal(User user) {
        this.user = Objects.requireNonNull(user, "user cannot be null");
    }

    public User getUser() {
        return user;
    }

    public String getUsername() {
        return user.getId();
    }
}
