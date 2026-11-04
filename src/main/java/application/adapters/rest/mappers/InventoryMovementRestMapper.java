package application.adapters.rest.mappers;

import application.adapters.rest.dto.response.InventoryMovementResponse;
import application.domain.models.InventoryMovement;
import org.springframework.stereotype.Component;

@Component
public class InventoryMovementRestMapper extends RestMapperSupport {

    public InventoryMovementResponse toResponse(InventoryMovement movement) {
        return new InventoryMovementResponse(
                movement.getId(),
                movement.getInventory().getId(),
                movement.getType().getCode(),
                movement.getQuantity(),
                movement.getPerformedBy().getId(),
                movement.getOccurredAt());
    }
}
