package application.adapters.persistence.jpa.adapters;

import application.adapters.persistence.jpa.entities.ReturnJpaEntity;
import application.adapters.persistence.jpa.mappers.ReturnJpaMapper;
import application.adapters.persistence.jpa.repositories.ReturnJpaRepository;
import application.domain.enums.ReturnStatus;
import application.domain.models.Return;
import application.domain.ports.out.ReturnRepositoryPort;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReturnJpaAdapter implements ReturnRepositoryPort {

    private final ReturnJpaRepository repository;
    private final ReturnJpaMapper mapper;
    private final JpaDependencyResolver resolver;

    @Override
    @Transactional
    public Return save(Return returnRequest) {
        ReturnJpaEntity saved = repository.save(mapper.toEntity(returnRequest));
        return mapper.toDomain(saved, returnRequest.getOrder());
    }

    @Override
    public Optional<Return> findById(String id) {
        return repository.findById(id).map(resolver::returnRequest);
    }

    @Override
    public List<Return> findByOrderId(String orderId) {
        return resolver.returns(repository.findByOrderId(orderId));
    }

    @Override
    public List<Return> findByBuyerId(String buyerId) {
        return resolver.returns(repository.findByBuyerId(buyerId));
    }

    @Override
    public List<Return> findAll() {
        return resolver.returns(repository.findAll());
    }

    @Override
    public List<Return> findByStatus(ReturnStatus status) {
        return resolver.returns(repository.findByStatusCode(status));
    }
}

