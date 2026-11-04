package application.adapters.rest.mappers;

import application.adapters.rest.dto.response.OrderItemResponse;
import application.adapters.rest.dto.response.OrderResponse;
import application.adapters.rest.dto.response.StockAllocationResponse;
import application.domain.models.CartItem;
import application.domain.models.Order;
import application.domain.valueobjects.StockAllocation;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderRestMapper extends RestMapperSupport {

    public OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(this::toResponse)
                .toList();
        List<StockAllocationResponse> allocations = order.getAllocations().stream()
                .map(this::toResponse)
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getBuyer().getId(),
                List.copyOf(items),
                order.getStatus().getCode(),
                toMoneyResponse(order.getTotalAmount()),
                order.getPaymentStatus().getCode(),
                List.copyOf(allocations),
                toAddressResponse(order.getDeliveryAddress()),
                order.getInvoice() == null ? null : order.getInvoice().getId(),
                order.getCreatedAt());
    }

    public OrderItemResponse toResponse(CartItem item) {
        return new OrderItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                toMoneyResponse(item.getUnitPrice()),
                toMoneyResponse(item.subtotal()));
    }

    public StockAllocationResponse toResponse(StockAllocation allocation) {
        return new StockAllocationResponse(
                allocation.getProductId(),
                allocation.getWarehouseId(),
                allocation.getQuantity());
    }
}
