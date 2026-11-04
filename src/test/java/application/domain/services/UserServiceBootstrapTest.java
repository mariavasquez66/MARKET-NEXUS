package application.domain.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import application.domain.enums.AuditAction;
import application.domain.enums.UserRole;
import application.domain.exceptions.common.InvalidDataException;
import application.domain.models.AuditLog;
import application.domain.models.User;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.PasswordServicePort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.valueobjects.Email;
import application.domain.valueobjects.FullName;
import application.domain.valueobjects.IdentityDocument;
import application.domain.valueobjects.PasswordHash;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceBootstrapTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private PasswordServicePort passwordService;

    @Mock
    private AuditPort auditPort;

    @Mock
    private ClockPort clockPort;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, passwordService, auditPort, clockPort);
    }

    @Test
    void createsActiveAdministratorAndAuditsTheEvent() {
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
        when(userRepository.existsByIdentityDocument(any(IdentityDocument.class))).thenReturn(false);
        when(passwordService.encode("secret-password")).thenReturn(PasswordHash.of("$2a$10$hash"));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clockPort.now()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));

        User admin = service.createBootstrapAdmin(
                FullName.of("Bootstrap Administrator"),
                Email.of("admin@nexusmarket.test"),
                IdentityDocument.of("BOOTSTRAP-ADMIN"),
                "secret-password");

        assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(admin.getStatus().getCode()).isEqualTo("ACTIVE");
        ArgumentCaptor<AuditLog> audit = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditPort).record(audit.capture());
        assertThat(audit.getValue().getAction()).isEqualTo(AuditAction.USER_CREATED);
        assertThat(audit.getValue().getEntityId()).isEqualTo(admin.getId());
    }

    @Test
    void refusesToCreateAnotherBootstrapAdministrator() {
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(true);

        assertThatThrownBy(() -> service.createBootstrapAdmin(
                FullName.of("Bootstrap Administrator"),
                Email.of("admin@nexusmarket.test"),
                IdentityDocument.of("BOOTSTRAP-ADMIN"),
                "secret-password"))
                .isInstanceOf(InvalidDataException.class);

        verify(passwordService, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void refusesBlankBootstrapPassword() {
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> service.createBootstrapAdmin(
                FullName.of("Bootstrap Administrator"),
                Email.of("admin@nexusmarket.test"),
                IdentityDocument.of("BOOTSTRAP-ADMIN"),
                " "))
                .isInstanceOf(InvalidDataException.class);
    }
}
