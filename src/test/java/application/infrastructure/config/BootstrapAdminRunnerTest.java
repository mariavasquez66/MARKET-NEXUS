package application.infrastructure.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import application.domain.DomainFixtures;
import application.domain.enums.UserRole;
import application.domain.models.User;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.services.UserService;
import application.domain.valueobjects.Email;
import application.domain.valueobjects.FullName;
import application.domain.valueobjects.IdentityDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BootstrapAdminRunnerTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private UserService userService;

    @BeforeEach
    void setUp() {
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
    }

    @Test
    void doesNothingWhenAnAdministratorAlreadyExists() {
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(true);
        BootstrapAdminRunner runner = runner("admin@nexusmarket.test", "secret-password");

        runner.run();

        verifyNoInteractions(userService);
    }

    @Test
    void doesNotCreateAdminWhenCredentialsAreMissing() {
        BootstrapAdminRunner withoutEmail = runner("", "secret-password");
        withoutEmail.run();

        BootstrapAdminRunner withoutPassword = runner("admin@nexusmarket.test", " ");
        withoutPassword.run();

        verify(userService, never()).createBootstrapAdmin(any(), any(), any(), any());
    }

    @Test
    void createsBootstrapAdminWhenCredentialsArePresent() {
        User admin = DomainFixtures.admin();
        when(userService.createBootstrapAdmin(any(FullName.class), any(Email.class),
                any(IdentityDocument.class), eq("secret-password"))).thenReturn(admin);
        BootstrapAdminRunner runner = runner("Admin@NexusMarket.test", "secret-password");

        runner.run();

        verify(userService).createBootstrapAdmin(
                FullName.of("Bootstrap Administrator"),
                Email.of("Admin@NexusMarket.test"),
                IdentityDocument.of("BOOTSTRAP-ADMIN"),
                "secret-password");
    }

    private BootstrapAdminRunner runner(String email, String password) {
        return new BootstrapAdminRunner(userRepository, userService, email, password);
    }
}
