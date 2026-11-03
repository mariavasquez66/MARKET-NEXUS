package application.adapters.rest.dto.response;

public record CartItemResponse(
        String productId,
        String productName,
        int quantity,
        MoneyResponse unitPrice,
        MoneyResponse subtotal) {
}
