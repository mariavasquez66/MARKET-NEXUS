package application.domain.services;

import static application.domain.DomainFixtures.admin;
import static application.domain.DomainFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import application.domain.enums.UserRole;
import application.domain.exceptions.common.InsufficientPermissionException;
import application.domain.exceptions.common.UnauthorizedOperationException;
import application.domain.exceptions.user.DuplicateEmailException;
import application.domain.exceptions.user.UserNotFoundException;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {
    @Mock UserRepositoryPort userRepository;
    @Mock PasswordServicePort passwordService;
    @Mock AuditPort auditPort;
    @Mock ClockPort clockPort;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, passwordService, auditPort, clockPort);
        when(clockPort.now()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void onlyAdminCanCreateAndListUsers() {
        assertThatThrownBy(() -> service.listUsers(user(UserRole.BUYER)))
                .isInstanceOf(InsufficientPermissionException.class);
        when(userRepository.findAll()).thenReturn(List.of());
        assertThat(service.listUsers(admin())).isEmpty();
    }

    @Test
    void duplicateEmailAndDocumentAreRejected() {
        User actor = admin();
        Email email = Email.of("new@test.com");
        IdentityDocument document = IdentityDocument.of("DOC12345");
        when(userRepository.existsByEmail(email)).thenReturn(true);
        assertThatThrownBy(() -> service.createUser(actor, FullName.of("New User"), email,
                document, UserRole.LOGISTIC_OPERATOR, "secret123"))
                .isInstanceOf(DuplicateEmailException.class);

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userRepository.existsByIdentityDocument(document)).thenReturn(true);
        assertThatThrownBy(() -> service.createUser(actor, FullName.of("New User"), email,
                document, UserRole.LOGISTIC_OPERATOR, "secret123"))
                .isInstanceOf(application.domain.exceptions.user.DuplicateIdentificationException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUserEncodesPasswordAndAudits() {
        User actor = admin();
        Email email = Email.of("new@test.com");
        IdentityDocument document = IdentityDocument.of("DOC12345");
        when(passwordService.encode("secret123")).thenReturn(PasswordHash.of("hash"));
        User created = service.createUser(actor, FullName.of("New User"), email,
                document, UserRole.LOGISTIC_OPERATOR, "secret123");
        assertThat(created.getRole()).isEqualTo(UserRole.LOGISTIC_OPERATOR);
        verify(auditPort).record(any());
    }

    @Test
    void adminCannotChangeOwnStatus() {
        User actor = admin();
        assertThatThrownBy(() -> service.blockUser(actor, actor.getId()))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void userCanReadOwnProfileButNotOthers() {
        User actor = user(UserRole.BUYER);
        when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));
        assertThat(service.getUser(actor, actor.getId())).isSameAs(actor);
        assertThatThrownBy(() -> service.getUser(actor, "other-id"))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void missingUserIsReported() {
        User actor = admin();
        when(userRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getUser(actor, "missing"))
                .isInstanceOf(UserNotFoundException.class);
    }
}
