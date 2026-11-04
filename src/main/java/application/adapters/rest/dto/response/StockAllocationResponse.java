package application.adapters.rest.dto.response;

public record StockAllocationResponse(
        String productId,
        String warehouseId,
        int quantity) {
}
