package application.adapters.persistence.jpa.adapters;

import application.adapters.persistence.jpa.entities.WarehouseJpaEntity;
import application.adapters.persistence.jpa.mappers.WarehouseJpaMapper;
import application.adapters.persistence.jpa.repositories.WarehouseJpaRepository;
import application.domain.models.Warehouse;
import application.domain.ports.out.WarehouseRepositoryPort;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class WarehouseJpaAdapter implements WarehouseRepositoryPort {

    private final WarehouseJpaRepository repository;
    private final WarehouseJpaMapper mapper;
    private final JpaDependencyResolver resolver;

    @Override
    @Transactional
    public Warehouse save(Warehouse warehouse) {
        WarehouseJpaEntity saved = repository.save(mapper.toEntity(warehouse));
        return mapper.toDomain(saved, warehouse.getOwner(), warehouse.getResponsible());
    }

    @Override
    public Optional<Warehouse> findById(String id) {
        return repository.findById(id).map(resolver::warehouse);
    }

    @Override
    public List<Warehouse> findAll() {
        return repository.findAll().stream().map(resolver::warehouse).toList();
    }

    @Override
    public List<Warehouse> findByOwnerSellerId(String sellerId) {
        return repository.findByOwnerSellerId(sellerId).stream().map(resolver::warehouse).toList();
    }

    @Override
    public boolean existsByOwnerSellerId(String sellerId) {
        return repository.existsByOwnerSellerId(sellerId);
    }
}
