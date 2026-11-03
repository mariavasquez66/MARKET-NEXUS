package application.adapters.persistence.jpa.adapters;

import application.adapters.persistence.jpa.entities.RefundJpaEntity;
import application.adapters.persistence.jpa.mappers.RefundJpaMapper;
import application.adapters.persistence.jpa.repositories.RefundJpaRepository;
import application.domain.models.Refund;
import application.domain.ports.out.RefundRepositoryPort;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RefundJpaAdapter implements RefundRepositoryPort {

    private final RefundJpaRepository repository;
    private final RefundJpaMapper mapper;
    private final JpaDependencyResolver resolver;

    @Override
    @Transactional
    public Refund save(Refund refund) {
        RefundJpaEntity saved = repository.save(mapper.toEntity(refund));
        return mapper.toDomain(saved, refund.getReturnRequest());
    }

    @Override
    public Optional<Refund> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Refund> findByReturnId(String returnId) {
        return repository.findByReturnId(returnId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Refund> findByBuyerId(String buyerId) {
        return repository.findByBuyerId(buyerId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Refund> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    private Refund toDomain(RefundJpaEntity entity) {
        return mapper.toDomain(entity, resolver.returnRequest(entity.getReturnId()));
    }
}
