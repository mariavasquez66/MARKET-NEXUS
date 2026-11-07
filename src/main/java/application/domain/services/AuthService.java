package application.domain.services;

import application.domain.enums.AuditAction;
import application.domain.enums.AuditEntityType;
import application.domain.enums.UserRole;
import application.domain.exceptions.common.InvalidDataException;
import application.domain.exceptions.common.UnauthenticatedUserException;
import application.domain.exceptions.user.DuplicateEmailException;
import application.domain.exceptions.user.DuplicateIdentificationException;
import application.domain.exceptions.user.UserNotFoundException;
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
import java.util.Objects;

public class AuthService {
    private final UserRepositoryPort userRepository;
    private final BuyerRepositoryPort buyerRepository;
    private final PasswordServicePort passwordService;
    private final JwtServicePort jwtService;
    private final AuditPort auditPort;
    private final ClockPort clockPort;

    public AuthService(UserRepositoryPort userRepository,
                       BuyerRepositoryPort buyerRepository,
                       PasswordServicePort passwordService,
                       JwtServicePort jwtService,
                       AuditPort auditPort,
                       ClockPort clockPort) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.buyerRepository = Objects.requireNonNull(buyerRepository);
        this.passwordService = Objects.requireNonNull(passwordService);
        this.jwtService = Objects.requireNonNull(jwtService);
        this.auditPort = Objects.requireNonNull(auditPort);
        this.clockPort = Objects.requireNonNull(clockPort);
    }

    public AuthResult login(String email, String rawPassword) {
        Email normalizedEmail = Email.of(email);
        requirePassword(rawPassword);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthenticatedUserException("Invalid credentials"));
        if (!passwordService.matches(rawPassword, user.getPasswordHash())) {
            throw new UnauthenticatedUserException("Invalid credentials");
        }
        user.assertCanOperate();
        ServiceSupport.audit(auditPort, clockPort, AuditAction.USER_LOGGED_IN,
                AuditEntityType.USER, user.getId(), user, "User logged in");
        return new AuthResult(jwtService.issueToken(user), user);
    }

    public void logout(User actor) {
        User user = requireUser(actor);
        user.invalidateTokens();
        userRepository.save(user);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.USER_LOGGED_OUT,
                AuditEntityType.USER, user.getId(), user, "User logged out");
    }

    public void changePassword(User actor, String currentPassword, String newPassword) {
        User user = requireUser(actor);
        requirePassword(currentPassword);
        PasswordPolicy.validate(newPassword);
        if (!passwordService.matches(currentPassword, user.getPasswordHash())) {
            throw new UnauthenticatedUserException("Current password is incorrect");
        }
        if (passwordService.matches(newPassword, user.getPasswordHash())) {
            throw new InvalidDataException("New password must differ from the current password");
        }
        user.changePasswordHash(passwordService.encode(newPassword));
        user.invalidateTokens();
        userRepository.save(user);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.PASSWORD_CHANGED,
                AuditEntityType.USER, user.getId(), user, "Password changed");
    }

    public User registerBuyer(FullName fullName,
                              Email email,
                              IdentityDocument identityDocument,
                              String rawPassword,
                              Address mainAddress) {
        Objects.requireNonNull(fullName, "fullName cannot be null");
        Objects.requireNonNull(email, "email cannot be null");
        Objects.requireNonNull(identityDocument, "identityDocument cannot be null");
        Objects.requireNonNull(mainAddress, "mainAddress cannot be null");
        PasswordPolicy.validate(rawPassword);
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("Email is already registered");
        }
        if (userRepository.existsByIdentityDocument(identityDocument)) {
            throw new DuplicateIdentificationException("Identity document is already registered");
        }
        PasswordHash passwordHash = passwordService.encode(rawPassword);
        User user = User.create(fullName, email, identityDocument, UserRole.BUYER, passwordHash);
        Buyer buyer = Buyer.create(user, mainAddress);
        userRepository.save(user);
        buyerRepository.save(buyer);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.USER_CREATED,
                AuditEntityType.USER, user.getId(), user, "Public buyer registration");
        return user;
    }

    private User requireUser(User actor) {
        Objects.requireNonNull(actor, "actor cannot be null");
        return userRepository.findById(actor.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + actor.getId()));
    }

    private static void requirePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new InvalidDataException("Password is required");
        }
    }
}
