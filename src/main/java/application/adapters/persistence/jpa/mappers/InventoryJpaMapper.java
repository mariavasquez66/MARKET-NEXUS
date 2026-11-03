package application.adapters.persistence.jpa.mappers;

import application.adapters.persistence.jpa.entities.InventoryJpaEntity;
import application.domain.models.Inventory;
import application.domain.models.Product;
import application.domain.models.Warehouse;
import org.springframework.stereotype.Component;

@Component
public class InventoryJpaMapper {

    public InventoryJpaEntity toEntity(Inventory inventory) {
        InventoryJpaEntity entity = new InventoryJpaEntity();
        entity.setId(inventory.getId());
        entity.setProductId(inventory.getProduct().getId());
        entity.setWarehouseId(inventory.getWarehouse().getId());
        entity.setAvailableQuantity(inventory.getAvailableQuantity());
        entity.setReservedQuantity(inventory.getReservedQuantity());
        entity.setConditionCode(inventory.getCondition());
        return entity;
    }

    public Inventory toDomain(InventoryJpaEntity entity, Product product, Warehouse warehouse) {
        return Inventory.reconstitute(
                entity.getId(),
                product,
                warehouse,
                entity.getAvailableQuantity(),
                entity.getReservedQuantity(),
                entity.getConditionCode());
    }
}
