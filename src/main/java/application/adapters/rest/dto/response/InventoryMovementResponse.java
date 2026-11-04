package application.adapters.rest.dto.response;

import java.time.Instant;

public record InventoryMovementResponse(
        String id,
        String inventoryId,
        String type,
        int quantity,
        String performedByUserId,
        Instant occurredAt) {
}
