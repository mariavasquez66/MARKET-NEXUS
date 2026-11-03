package application.adapters.persistence.jpa.repositories;

import application.adapters.persistence.jpa.entities.UserJpaEntity;
import application.domain.enums.UserRole;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, String> {

    Optional<UserJpaEntity> findByEmailValue(String emailValue);

    boolean existsByEmailValue(String emailValue);

    boolean existsByIdentityDocumentValue(String identityDocumentValue);

    boolean existsByRoleCode(UserRole roleCode);
}
