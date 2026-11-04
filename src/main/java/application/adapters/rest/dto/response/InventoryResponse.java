package application.adapters.rest.dto.response;

public record InventoryResponse(
        String id,
        String productId,
        String warehouseId,
        int availableQuantity,
        int reservedQuantity,
        String condition) {
}
