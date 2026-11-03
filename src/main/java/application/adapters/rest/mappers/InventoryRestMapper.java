package application.adapters.rest.mappers;

import application.adapters.rest.dto.response.InventoryResponse;
import application.domain.models.Inventory;
import org.springframework.stereotype.Component;

@Component
public class InventoryRestMapper extends RestMapperSupport {

    public InventoryResponse toResponse(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getProduct().getId(),
                inventory.getWarehouse().getId(),
                inventory.getAvailableQuantity(),
                inventory.getReservedQuantity(),
                inventory.getCondition().getCode());
    }
}
