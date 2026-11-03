package application.adapters.rest.dto.request;

public record InventoryAdjustmentRequest(
        Integer quantity,
        Integer delta) {
}
