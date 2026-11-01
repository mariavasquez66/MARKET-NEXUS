package application.domain.ports.out;

import application.domain.models.User;

public interface JwtServicePort {
    String issueToken(User user);
}
