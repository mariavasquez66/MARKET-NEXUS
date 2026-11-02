package application.adapters.persistence.jpa.adapters;

import application.adapters.persistence.jpa.entities.InventoryJpaEntity;
import application.adapters.persistence.jpa.mappers.InventoryJpaMapper;
import application.adapters.persistence.jpa.repositories.InventoryJpaRepository;
import application.domain.models.Inventory;
import application.domain.ports.out.InventoryRepositoryPort;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class InventoryJpaAdapter implements InventoryRepositoryPort {

    private final InventoryJpaRepository repository;
    private final InventoryJpaMapper mapper;
    private final JpaDependencyResolver resolver;

    @Override
    @Transactional
    public Inventory save(Inventory inventory) {
        InventoryJpaEntity saved = repository.save(mapper.toEntity(inventory));
        return mapper.toDomain(saved, inventory.getProduct(), inventory.getWarehouse());
    }

    @Override
    public Optional<Inventory> findById(String id) {
        return repository.findById(id).map(resolver::inventory);
    }

    @Override
    public Optional<Inventory> findByProductIdAndWarehouseId(String productId, String warehouseId) {
        return repository.findByProductIdAndWarehouseId(productId, warehouseId)
                .map(resolver::inventory);
    }

    @Override
    public List<Inventory> findByProductId(String productId) {
        return repository.findByProductId(productId).stream().map(resolver::inventory).toList();
    }

    @Override
    public List<Inventory> findByWarehouseId(String warehouseId) {
        return repository.findByWarehouseId(warehouseId).stream().map(resolver::inventory).toList();
    }

    @Override
    public List<Inventory> findBySellerId(String sellerId) {
        return repository.findByWarehouseOwnerSellerId(sellerId).stream()
                .map(resolver::inventory)
                .toList();
    }

    @Override
    public List<Inventory> findAll() {
        return repository.findAll().stream().map(resolver::inventory).toList();
    }

    @Override
    public boolean existsByProductId(String productId) {
        return repository.existsByProductId(productId);
    }
}
