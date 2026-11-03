package application.adapters.rest.dto.response;

import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String id,
        String buyerId,
        List<OrderItemResponse> items,
        String status,
        MoneyResponse totalAmount,
        String paymentStatus,
        List<StockAllocationResponse> allocations,
        AddressResponse deliveryAddress,
        String invoiceId,
        Instant createdAt) {
}
