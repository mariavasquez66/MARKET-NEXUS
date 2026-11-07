package application.adapters.persistence.jpa.mappers;

import application.adapters.persistence.jpa.entities.AddressEmbeddable;
import application.adapters.persistence.jpa.entities.OrderAllocationJpaEntity;
import application.adapters.persistence.jpa.entities.OrderItemJpaEntity;
import application.adapters.persistence.jpa.entities.OrderJpaEntity;
import application.domain.models.Buyer;
import application.domain.models.CartItem;
import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.valueobjects.Address;
import application.domain.valueobjects.Money;
import application.domain.valueobjects.StockAllocation;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OrderJpaMapper {

    public OrderJpaEntity toEntity(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity();
        entity.setId(order.getId());
        return updateEntity(entity, order);
    }

    public OrderJpaEntity updateEntity(OrderJpaEntity entity, Order order) {
        entity.setBuyerId(order.getBuyer().getId());
        entity.setStatusCode(order.getStatus());
        entity.setTotalAmount(order.getTotalAmount().getAmount());
        entity.setTotalCurrency(order.getTotalAmount().getCurrency().getCurrencyCode());
        entity.setPaymentStatusCode(order.getPaymentStatus());
        entity.setDeliveryAddress(toEmbeddable(order.getDeliveryAddress()));
        entity.setInvoiceId(order.getInvoice() == null ? null : order.getInvoice().getId());
        entity.setCreatedAt(order.getCreatedAt());
        entity.setPaidAt(order.getPaidAt());
        return entity;
    }

    public Order toDomain(OrderJpaEntity entity,
                          Buyer buyer,
                          List<CartItem> items,
                          List<StockAllocation> allocations,
                          Invoice invoice) {
        return Order.reconstitute(
                entity.getId(),
                buyer,
                items,
                entity.getStatusCode(),
                Money.of(entity.getTotalAmount(), Currency.getInstance(entity.getTotalCurrency())),
                entity.getPaymentStatusCode(),
                allocations,
                toAddress(entity.getDeliveryAddress()),
                invoice,
                entity.getCreatedAt(),
                entity.getPaidAt());
    }

    public OrderItemJpaEntity toEntity(CartItem item, String orderId) {
        OrderItemJpaEntity entity = new OrderItemJpaEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setOrderId(orderId);
        entity.setProductId(item.getProduct().getId());
        entity.setQuantity(item.getQuantity());
        entity.setUnitPriceAmount(item.getUnitPrice().getAmount());
        entity.setUnitPriceCurrency(item.getUnitPrice().getCurrency().getCurrencyCode());
        return entity;
    }

    public CartItem toDomainOrderItem(OrderItemJpaEntity entity, Product product) {
        Money unitPrice = Money.of(
                entity.getUnitPriceAmount(),
                Currency.getInstance(entity.getUnitPriceCurrency()));
        return CartItem.reconstitute(product, entity.getQuantity(), unitPrice);
    }

    public OrderAllocationJpaEntity toEntity(StockAllocation allocation, String orderId) {
        OrderAllocationJpaEntity entity = new OrderAllocationJpaEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setOrderId(orderId);
        entity.setProductId(allocation.getProductId());
        entity.setWarehouseId(allocation.getWarehouseId());
        entity.setQuantity(allocation.getQuantity());
        return entity;
    }

    public StockAllocation toDomain(OrderAllocationJpaEntity entity) {
        return StockAllocation.of(
                entity.getProductId(),
                entity.getWarehouseId(),
                entity.getQuantity());
    }

    private AddressEmbeddable toEmbeddable(Address address) {
        AddressEmbeddable embeddable = new AddressEmbeddable();
        embeddable.setStreet(address.getStreet());
        embeddable.setCity(address.getCity());
        embeddable.setRegion(address.getRegion());
        embeddable.setPostalCode(address.getPostalCode());
        embeddable.setCountry(address.getCountry());
        return embeddable;
    }

    private Address toAddress(AddressEmbeddable embeddable) {
        return Address.of(
                embeddable.getStreet(),
                embeddable.getCity(),
                embeddable.getRegion(),
                embeddable.getPostalCode(),
                embeddable.getCountry());
    }
}
