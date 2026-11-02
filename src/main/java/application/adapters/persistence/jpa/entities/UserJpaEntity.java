package application.adapters.persistence.jpa.entities;

import application.adapters.persistence.jpa.mappers.converters.UserRoleConverter;
import application.adapters.persistence.jpa.mappers.converters.UserStatusConverter;
import application.domain.enums.UserRole;
import application.domain.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "full_name", length = 120, nullable = false)
    private String fullNameValue;

    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String emailValue;

    @Column(name = "identity_document", length = 30, nullable = false, unique = true)
    private String identityDocumentValue;

    @Convert(converter = UserRoleConverter.class)
    @Column(name = "role", length = 40, nullable = false)
    private UserRole roleCode;

    @Convert(converter = UserStatusConverter.class)
    @Column(name = "status", length = 40, nullable = false)
    private UserStatus statusCode;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHashValue;

    @Column(name = "auth_token_version", nullable = false)
    private long authTokenVersion;
}
