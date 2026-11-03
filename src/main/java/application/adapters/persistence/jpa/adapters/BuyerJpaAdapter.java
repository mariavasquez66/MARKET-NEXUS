package application.adapters.persistence.jpa.adapters;

import application.adapters.persistence.jpa.entities.BuyerJpaEntity;
import application.adapters.persistence.jpa.mappers.BuyerJpaMapper;
import application.adapters.persistence.jpa.repositories.BuyerJpaRepository;
import application.domain.models.Buyer;
import application.domain.ports.out.BuyerRepositoryPort;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BuyerJpaAdapter implements BuyerRepositoryPort {

    private final BuyerJpaRepository repository;
    private final BuyerJpaMapper mapper;
    private final JpaDependencyResolver resolver;

    @Override
    @Transactional
    public Buyer save(Buyer buyer) {
        BuyerJpaEntity saved = repository.save(mapper.toEntity(buyer));
        return mapper.toDomain(saved, resolver.user(saved.getUserId()));
    }

    @Override
    public Optional<Buyer> findById(String id) {
        return repository.findById(id).map(resolver::buyer);
    }

    @Override
    public Optional<Buyer> findByUserId(String userId) {
        return repository.findByUserId(userId).map(resolver::buyer);
    }

    @Override
    public List<Buyer> findAll() {
        return repository.findAll().stream().map(resolver::buyer).toList();
    }
}
