package application.domain.models;

import application.domain.enums.UserRole;
import application.domain.enums.UserStatus;
import application.domain.exceptions.common.InvalidDataException;
import application.domain.exceptions.user.InactiveUserException;
import application.domain.valueobjects.Email;
import application.domain.valueobjects.FullName;
import application.domain.valueobjects.IdentityDocument;
import application.domain.valueobjects.PasswordHash;
import java.util.Objects;
import java.util.UUID;

public class User {
    private final String id;
    private FullName fullName;
    private Email email;
    private IdentityDocument identityDocument;
    private final UserRole role;
    private UserStatus status;
    private PasswordHash passwordHash;
    private long authTokenVersion;

    private User(String id,
                 FullName fullName,
                 Email email,
                 IdentityDocument identityDocument,
                 UserRole role,
                 UserStatus status,
                 PasswordHash passwordHash,
                 long authTokenVersion) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.fullName = Objects.requireNonNull(fullName, "fullName cannot be null");
        this.email = Objects.requireNonNull(email, "email cannot be null");
        this.identityDocument = Objects.requireNonNull(identityDocument, "identityDocument cannot be null");
        this.role = Objects.requireNonNull(role, "role cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash cannot be null");
        if (authTokenVersion < 0) {
            throw new InvalidDataException("authTokenVersion cannot be negative");
        }
        this.authTokenVersion = authTokenVersion;
    }

    public static User create(FullName fullName,
                              Email email,
                              IdentityDocument identityDocument,
                              UserRole role,
                              PasswordHash passwordHash) {
        return new User(UUID.randomUUID().toString(), fullName, email, identityDocument,
                role, UserStatus.ACTIVE, passwordHash, 0L);
    }

    public static User reconstitute(String id,
                                    FullName fullName,
                                    Email email,
                                    IdentityDocument identityDocument,
                                    UserRole role,
                                    UserStatus status,
                                    PasswordHash passwordHash,
                                    long authTokenVersion) {
        return new User(id, fullName, email, identityDocument, role, status, passwordHash, authTokenVersion);
    }

    public void block() {
        if (status != UserStatus.ACTIVE) {
            throw new InvalidDataException("Only active users can be blocked");
        }
        status = UserStatus.BLOCKED;
        invalidateTokens();
    }

    public void activate() {
        if (status == UserStatus.ACTIVE) {
            throw new InvalidDataException("User is already active");
        }
        status = UserStatus.ACTIVE;
        invalidateTokens();
    }

    public void deactivate() {
        if (status == UserStatus.INACTIVE) {
            throw new InvalidDataException("User is already inactive");
        }
        status = UserStatus.INACTIVE;
        invalidateTokens();
    }

    public void invalidateTokens() {
        authTokenVersion++;
    }

    public void assertCanOperate() {
        if (status != UserStatus.ACTIVE) {
            throw new InactiveUserException("User is not active: " + id);
        }
    }

    public boolean hasRole(UserRole role) {
        return this.role.equals(role);
    }

    public void changeFullName(FullName fullName) {
        this.fullName = Objects.requireNonNull(fullName, "fullName cannot be null");
    }

    public void changeEmail(Email email) {
        this.email = Objects.requireNonNull(email, "email cannot be null");
    }

    public void changeIdentityDocument(IdentityDocument identityDocument) {
        this.identityDocument = Objects.requireNonNull(identityDocument, "identityDocument cannot be null");
    }

    public void changePasswordHash(PasswordHash passwordHash) {
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash cannot be null");
    }

    public String getId() {
        return id;
    }

    public FullName getFullName() {
        return fullName;
    }

    public Email getEmail() {
        return email;
    }

    public IdentityDocument getIdentityDocument() {
        return identityDocument;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public PasswordHash getPasswordHash() {
        return passwordHash;
    }

    public long getAuthTokenVersion() {
        return authTokenVersion;
    }
}
