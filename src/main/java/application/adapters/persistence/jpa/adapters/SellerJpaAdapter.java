package application.adapters.persistence.jpa.adapters;

import application.adapters.persistence.jpa.entities.SellerJpaEntity;
import application.adapters.persistence.jpa.mappers.SellerJpaMapper;
import application.adapters.persistence.jpa.repositories.SellerJpaRepository;
import application.domain.models.Seller;
import application.domain.ports.out.SellerRepositoryPort;
import application.domain.valueobjects.TaxId;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SellerJpaAdapter implements SellerRepositoryPort {

    private final SellerJpaRepository repository;
    private final SellerJpaMapper mapper;
    private final JpaDependencyResolver resolver;

    @Override
    @Transactional
    public Seller save(Seller seller) {
        SellerJpaEntity saved = repository.save(mapper.toEntity(seller));
        return mapper.toDomain(saved, seller.getUser(), seller.getWarehouses());
    }

    @Override
    public Optional<Seller> findById(String id) {
        return repository.findById(id).map(resolver::seller);
    }

    @Override
    public Optional<Seller> findByUserId(String userId) {
        return repository.findByUserId(userId).map(resolver::seller);
    }

    @Override
    public List<Seller> findAll() {
        return repository.findAll().stream().map(resolver::seller).toList();
    }

    @Override
    public boolean existsByTaxId(TaxId taxId) {
        return repository.existsByTaxIdValue(taxId.getValue());
    }
}
