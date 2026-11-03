package application.adapters.persistence.jpa.mappers;

import application.adapters.persistence.jpa.entities.UserJpaEntity;
import application.domain.models.User;
import application.domain.valueobjects.Email;
import application.domain.valueobjects.FullName;
import application.domain.valueobjects.IdentityDocument;
import application.domain.valueobjects.PasswordHash;
import org.springframework.stereotype.Component;

@Component
public class UserJpaMapper {

    public UserJpaEntity toEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId());
        entity.setFullNameValue(user.getFullName().getValue());
        entity.setEmailValue(user.getEmail().getValue());
        entity.setIdentityDocumentValue(user.getIdentityDocument().getValue());
        entity.setRoleCode(user.getRole());
        entity.setStatusCode(user.getStatus());
        entity.setPasswordHashValue(user.getPasswordHash().getValue());
        entity.setAuthTokenVersion(user.getAuthTokenVersion());
        return entity;
    }

    public User toDomain(UserJpaEntity entity) {
        return User.reconstitute(
                entity.getId(),
                FullName.of(entity.getFullNameValue()),
                Email.of(entity.getEmailValue()),
                IdentityDocument.of(entity.getIdentityDocumentValue()),
                entity.getRoleCode(),
                entity.getStatusCode(),
                PasswordHash.of(entity.getPasswordHashValue()),
                entity.getAuthTokenVersion());
    }
}
