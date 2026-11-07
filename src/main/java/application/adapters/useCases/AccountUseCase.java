package application.adapters.useCases;

import application.domain.models.User;
import application.domain.ports.in.AccountPort;
import application.domain.services.AuthService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AccountUseCase implements AccountPort {

    private final AuthService authService;

    public AccountUseCase(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void logout(User actor) {
        authService.logout(actor);
    }

    @Override
    public void changePassword(User actor, String currentPassword, String newPassword) {
        authService.changePassword(actor, currentPassword, newPassword);
    }
}
