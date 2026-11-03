package application.adapters.persistence.jpa.adapters;

import application.adapters.persistence.jpa.entities.InventoryMovementJpaEntity;
import application.adapters.persistence.jpa.mappers.InventoryMovementJpaMapper;
import application.adapters.persistence.jpa.repositories.InventoryMovementJpaRepository;
import application.domain.models.InventoryMovement;
import application.domain.ports.out.InventoryMovementRepositoryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class InventoryMovementJpaAdapter implements InventoryMovementRepositoryPort {

    private final InventoryMovementJpaRepository repository;
    private final InventoryMovementJpaMapper mapper;
    private final JpaDependencyResolver resolver;

    @Override
    @Transactional
    public InventoryMovement save(InventoryMovement movement) {
        InventoryMovementJpaEntity saved = repository.save(mapper.toEntity(movement));
        return mapper.toDomain(saved, movement.getInventory(), movement.getPerformedBy());
    }

    @Override
    public List<InventoryMovement> findByInventoryId(String inventoryId) {
        return repository.findByInventoryId(inventoryId).stream()
                .map(entity -> mapper.toDomain(
                        entity,
                        resolver.inventory(entity.getInventoryId()),
                        resolver.user(entity.getPerformedByUserId())))
                .toList();
    }

    @Override
    public List<InventoryMovement> findAll() {
        return repository.findAll().stream()
                .map(entity -> mapper.toDomain(
                        entity,
                        resolver.inventory(entity.getInventoryId()),
                        resolver.user(entity.getPerformedByUserId())))
                .toList();
    }
}
