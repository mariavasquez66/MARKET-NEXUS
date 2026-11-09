package application.domain.ports.in;

import application.domain.models.User;

public interface AccountPort {
    void logout(User actor);

    void changePassword(User actor, String currentPassword, String newPassword);
}
