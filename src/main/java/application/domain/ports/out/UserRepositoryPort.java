package application.domain.ports.out;

import application.domain.enums.UserRole;
import application.domain.models.User;
import application.domain.valueobjects.Email;
import application.domain.valueobjects.IdentityDocument;
import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {
    User save(User user);

    Optional<User> findById(String id);

    Optional<User> findByEmail(Email email);

    List<User> findAll();

    boolean existsByEmail(Email email);

    boolean existsByIdentityDocument(IdentityDocument identityDocument);

    boolean existsByRole(UserRole role);
}
