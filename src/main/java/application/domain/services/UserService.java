package application.domain.services;

import application.domain.enums.AuditAction;
import application.domain.enums.AuditEntityType;
import application.domain.enums.UserRole;
import application.domain.exceptions.common.InvalidDataException;
import application.domain.exceptions.common.UnauthorizedOperationException;
import application.domain.exceptions.user.DuplicateEmailException;
import application.domain.exceptions.user.DuplicateIdentificationException;
import application.domain.exceptions.user.UserNotFoundException;
import application.domain.models.User;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.PasswordServicePort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.valueobjects.Email;
import application.domain.valueobjects.FullName;
import application.domain.valueobjects.IdentityDocument;
import java.util.List;
import java.util.Objects;

public class UserService {
    private final UserRepositoryPort userRepository;
    private final PasswordServicePort passwordService;
    private final AuditPort auditPort;
    private final ClockPort clockPort;

    public UserService(UserRepositoryPort userRepository,
                       PasswordServicePort passwordService,
                       AuditPort auditPort,
                       ClockPort clockPort) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.passwordService = Objects.requireNonNull(passwordService);
        this.auditPort = Objects.requireNonNull(auditPort);
        this.clockPort = Objects.requireNonNull(clockPort);
    }

    public User createUser(User actor,
                           FullName fullName,
                           Email email,
                           IdentityDocument identityDocument,
                           UserRole role,
                           String rawPassword) {
        ServiceSupport.requireRole(actor, UserRole.ADMIN);
        PasswordPolicy.validate(rawPassword);
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("Email is already registered");
        }
        if (userRepository.existsByIdentityDocument(identityDocument)) {
            throw new DuplicateIdentificationException("Identity document is already registered");
        }
        User user = User.create(fullName, email, identityDocument, role,
                passwordService.encode(rawPassword));
        User saved = userRepository.save(user);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.USER_CREATED,
                AuditEntityType.USER, saved.getId(), actor, "User created by admin");
        return saved;
    }

    public User createBootstrapAdmin(FullName fullName,
                                     Email email,
                                     IdentityDocument identityDocument,
                                     String rawPassword) {
        Objects.requireNonNull(fullName, "fullName cannot be null");
        Objects.requireNonNull(email, "email cannot be null");
        Objects.requireNonNull(identityDocument, "identityDocument cannot be null");
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            throw new InvalidDataException("An administrator already exists");
        }
        PasswordPolicy.validate(rawPassword);
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("Email is already registered");
        }
        if (userRepository.existsByIdentityDocument(identityDocument)) {
            throw new DuplicateIdentificationException("Identity document is already registered");
        }
        User user = User.create(fullName, email, identityDocument, UserRole.ADMIN,
                passwordService.encode(rawPassword));
        User saved = userRepository.save(user);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.USER_CREATED,
                AuditEntityType.USER, saved.getId(), saved, "Bootstrap administrator creation");
        return saved;
    }

    public List<User> listUsers(User actor) {
        ServiceSupport.requireRole(actor, UserRole.ADMIN);
        return userRepository.findAll();
    }

    public User getUser(User actor, String userId) {
        Objects.requireNonNull(actor, "actor cannot be null");
        actor.assertCanOperate();
        if (!actor.hasRole(UserRole.ADMIN) && !actor.getId().equals(userId)) {
            throw new UnauthorizedOperationException("Cannot access another user profile");
        }
        return requireUser(userId);
    }

    public User updateUserProfile(User actor,
                                  String userId,
                                  FullName fullName,
                                  Email email) {
        Objects.requireNonNull(actor, "actor cannot be null");
        actor.assertCanOperate();
        if (!actor.hasRole(UserRole.ADMIN) && !actor.getId().equals(userId)) {
            throw new UnauthorizedOperationException("Cannot update another user profile");
        }
        User user = requireUser(userId);
        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("Email is already registered");
        }
        user.changeFullName(fullName);
        user.changeEmail(email);
        return userRepository.save(user);
    }

    public User blockUser(User actor, String userId) {
        return changeStatus(actor, userId, AuditAction.USER_BLOCKED, "User blocked", UserStatusAction.BLOCK);
    }

    public User activateUser(User actor, String userId) {
        return changeStatus(actor, userId, AuditAction.USER_ACTIVATED, "User activated", UserStatusAction.ACTIVATE);
    }

    public User deactivateUser(User actor, String userId) {
        return changeStatus(actor, userId, AuditAction.USER_DEACTIVATED, "User deactivated", UserStatusAction.DEACTIVATE);
    }

    private User changeStatus(User actor,
                              String userId,
                              AuditAction action,
                              String details,
                              UserStatusAction statusAction) {
        ServiceSupport.requireRole(actor, UserRole.ADMIN);
        if (actor.getId().equals(userId)) {
            throw new UnauthorizedOperationException("Administrators cannot change their own status");
        }
        User user = requireUser(userId);
        switch (statusAction) {
            case BLOCK -> user.block();
            case ACTIVATE -> user.activate();
            case DEACTIVATE -> user.deactivate();
        }
        User saved = userRepository.save(user);
        ServiceSupport.audit(auditPort, clockPort, action, AuditEntityType.USER,
                saved.getId(), actor, details);
        return saved;
    }

    private User requireUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    }

    private enum UserStatusAction {
        BLOCK, ACTIVATE, DEACTIVATE
    }
}
