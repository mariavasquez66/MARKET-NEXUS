package application.adapters.persistence.jpa.adapters;

import application.adapters.persistence.jpa.entities.UserJpaEntity;
import application.adapters.persistence.jpa.mappers.UserJpaMapper;
import application.adapters.persistence.jpa.repositories.UserJpaRepository;
import application.domain.enums.UserRole;
import application.domain.models.User;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.valueobjects.Email;
import application.domain.valueobjects.IdentityDocument;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserJpaAdapter implements UserRepositoryPort {

    private final UserJpaRepository repository;
    private final UserJpaMapper mapper;

    @Override
    @Transactional
    public User save(User user) {
        UserJpaEntity saved = repository.save(mapper.toEntity(user));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(String id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return repository.findByEmailValue(email.getValue()).map(mapper::toDomain);
    }

    @Override
    public List<User> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByEmail(Email email) {
        return repository.existsByEmailValue(email.getValue());
    }

    @Override
    public boolean existsByIdentityDocument(IdentityDocument identityDocument) {
        return repository.existsByIdentityDocumentValue(identityDocument.getValue());
    }

    @Override
    public boolean existsByRole(UserRole role) {
        return repository.existsByRoleCode(role.getCode());
    }
}
