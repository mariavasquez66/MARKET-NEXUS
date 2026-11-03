package application.domain.services;

import static application.domain.DomainFixtures.address;
import static application.domain.DomainFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import application.domain.enums.UserRole;
import application.domain.exceptions.common.UnauthenticatedUserException;
import application.domain.exceptions.user.DuplicateEmailException;
import application.domain.exceptions.user.DuplicateIdentificationException;
import application.domain.models.AuthResult;
import application.domain.models.Buyer;
import application.domain.models.User;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.BuyerRepositoryPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.JwtServicePort;
import application.domain.ports.out.PasswordServicePort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.valueobjects.Address;
import application.domain.valueobjects.Email;
import application.domain.valueobjects.FullName;
import application.domain.valueobjects.IdentityDocument;
import application.domain.valueobjects.PasswordHash;
import java.time.Instant;
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
class AuthServiceTest {
    @Mock UserRepositoryPort userRepository;
    @Mock BuyerRepositoryPort buyerRepository;
    @Mock PasswordServicePort passwordService;
    @Mock JwtServicePort jwtService;
    @Mock AuditPort auditPort;
    @Mock ClockPort clockPort;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, buyerRepository, passwordService,
                jwtService, auditPort, clockPort);
        when(clockPort.now()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void loginRejectsUnknownEmailAndWrongPasswordWithSameMessage() {
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.login("nobody@test.com", "secret"))
                .isInstanceOf(UnauthenticatedUserException.class)
                .hasMessage("Invalid credentials");

        User user = user(UserRole.BUYER);
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordService.matches("secret", user.getPasswordHash())).thenReturn(false);
        assertThatThrownBy(() -> service.login(user.getEmail().getValue(), "secret"))
                .isInstanceOf(UnauthenticatedUserException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void loginReturnsTokenForActiveUser() {
        User user = user(UserRole.BUYER);
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordService.matches("secret", user.getPasswordHash())).thenReturn(true);
        when(jwtService.issueToken(user)).thenReturn("jwt-token");
        AuthResult result = service.login(user.getEmail().getValue(), "secret");
        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.user()).isSameAs(user);
    }

    @Test
    void registerBuyerRejectsDuplicates() {
        Email email = Email.of("new@test.com");
        IdentityDocument document = IdentityDocument.of("DOC12345");
        when(userRepository.existsByEmail(email)).thenReturn(true);
        assertThatThrownBy(() -> register(email, document))
                .isInstanceOf(DuplicateEmailException.class);

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userRepository.existsByIdentityDocument(document)).thenReturn(true);
        assertThatThrownBy(() -> register(email, document))
                .isInstanceOf(DuplicateIdentificationException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerBuyerCreatesUserAndBuyerAndAudits() {
        Email email = Email.of("new@test.com");
        IdentityDocument document = IdentityDocument.of("DOC12345");
        when(passwordService.encode("secret")).thenReturn(PasswordHash.of("hash"));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(buyerRepository.save(any(Buyer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = register(email, document);

        assertThat(created.getRole()).isEqualTo(UserRole.BUYER);
        verify(buyerRepository).save(any(Buyer.class));
        verify(auditPort).record(any());
    }

    private User register(Email email, IdentityDocument document) {
        return service.registerBuyer(FullName.of("New Buyer"), email, document,
                "secret", address());
    }
}
