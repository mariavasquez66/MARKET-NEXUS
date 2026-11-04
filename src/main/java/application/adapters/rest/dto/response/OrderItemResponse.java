package application.adapters.rest.dto.response;

public record OrderItemResponse(
        String productId,
        String productName,
        int quantity,
        MoneyResponse unitPrice,
        MoneyResponse subtotal) {
}
