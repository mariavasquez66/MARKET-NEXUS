package application.adapters.persistence.jpa.mappers;

import application.adapters.persistence.jpa.entities.InventoryMovementJpaEntity;
import application.domain.models.Inventory;
import application.domain.models.InventoryMovement;
import application.domain.models.User;
import org.springframework.stereotype.Component;

@Component
public class InventoryMovementJpaMapper {

    public InventoryMovementJpaEntity toEntity(InventoryMovement movement) {
        InventoryMovementJpaEntity entity = new InventoryMovementJpaEntity();
        entity.setId(movement.getId());
        entity.setInventoryId(movement.getInventory().getId());
        entity.setTypeCode(movement.getType());
        entity.setQuantity(movement.getQuantity());
        entity.setPerformedByUserId(movement.getPerformedBy().getId());
        entity.setOccurredAt(movement.getOccurredAt());
        return entity;
    }

    public InventoryMovement toDomain(InventoryMovementJpaEntity entity,
                                      Inventory inventory,
                                      User performedBy) {
        return InventoryMovement.reconstitute(
                entity.getId(),
                inventory,
                entity.getTypeCode(),
                entity.getQuantity(),
                performedBy,
                entity.getOccurredAt());
    }
}
