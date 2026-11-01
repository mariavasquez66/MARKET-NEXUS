package application.domain.models;

import application.domain.enums.OrderStatus;
import application.domain.enums.PaymentStatus;
import application.domain.exceptions.common.InvalidDataException;
import application.domain.exceptions.order.CancelledOrderException;
import application.domain.exceptions.order.FinalizedOrderException;
import application.domain.exceptions.order.InvalidStateTransitionException;
import application.domain.valueobjects.Address;
import application.domain.valueobjects.Money;
import application.domain.valueobjects.StockAllocation;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Order {
    private final String id;
    private final Buyer buyer;
    private final List<CartItem> items;
    private OrderStatus status;
    private final Money totalAmount;
    private PaymentStatus paymentStatus;
    private final List<StockAllocation> allocations;
    private final Address deliveryAddress;
    private Invoice invoice;
    private final Instant createdAt;

    private Order(String id,
                  Buyer buyer,
                  List<CartItem> items,
                  OrderStatus status,
                  Money totalAmount,
                  PaymentStatus paymentStatus,
                  List<StockAllocation> allocations,
                  Address deliveryAddress,
                  Invoice invoice,
                  Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.buyer = Objects.requireNonNull(buyer, "buyer cannot be null");
        this.items = new ArrayList<>(Objects.requireNonNull(items));
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.totalAmount = Objects.requireNonNull(totalAmount, "totalAmount cannot be null");
        this.paymentStatus = Objects.requireNonNull(paymentStatus, "paymentStatus cannot be null");
        this.allocations = new ArrayList<>(Objects.requireNonNull(allocations));
        this.deliveryAddress = Objects.requireNonNull(deliveryAddress, "deliveryAddress cannot be null");
        this.invoice = invoice;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }

    public static Order fromCart(Cart cart, Address deliveryAddress, Instant createdAt) {
        Objects.requireNonNull(cart, "cart cannot be null");
        cart.assertNotEmpty();
        cart.getBuyer().assertCanPurchase();
        return new Order(UUID.randomUUID().toString(), cart.getBuyer(),
                cart.getItems(), OrderStatus.PENDING_PAYMENT, cart.total(),
                PaymentStatus.PENDING, List.of(), deliveryAddress, null, createdAt);
    }

    public static Order reconstitute(String id,
                                     Buyer buyer,
                                     List<CartItem> items,
                                     OrderStatus status,
                                     Money totalAmount,
                                     PaymentStatus paymentStatus,
                                     List<StockAllocation> allocations,
                                     Address deliveryAddress,
                                     Invoice invoice,
                                     Instant createdAt) {
        return new Order(id, buyer, items, status, totalAmount, paymentStatus,
                allocations, deliveryAddress, invoice, createdAt);
    }

    public void assignAllocations(List<StockAllocation> allocations) {
        assertModifiable();
        if (allocations == null || allocations.isEmpty()) {
            throw new InvalidDataException("Order requires at least one allocation");
        }
        this.allocations.clear();
        this.allocations.addAll(allocations);
    }

    public void confirmPayment(Instant paidAt) {
        if (!status.equals(OrderStatus.PENDING_PAYMENT)) {
            throw new InvalidStateTransitionException("Payment can only be confirmed from PENDING_PAYMENT");
        }
        Objects.requireNonNull(paidAt, "paidAt cannot be null");
        status = OrderStatus.PAID;
        paymentStatus = PaymentStatus.CONFIRMED;
    }

    public void markDispatched() {
        if (!status.equals(OrderStatus.PAID)) {
            throw new InvalidStateTransitionException("Order can only be dispatched from PAID");
        }
        if (isDigitalOnly()) {
            throw new InvalidStateTransitionException("Digital orders cannot be dispatched");
        }
        status = OrderStatus.DISPATCHED;
    }

    public void markDelivered() {
        if (status.equals(OrderStatus.PAID) && isDigitalOnly()) {
            status = OrderStatus.DELIVERED;
            return;
        }
        if (status.equals(OrderStatus.DISPATCHED)) {
            status = OrderStatus.DELIVERED;
            return;
        }
        throw new InvalidStateTransitionException("Order cannot be delivered from " + status.getCode());
    }

    public void finalizeOrder() {
        if (!status.equals(OrderStatus.DELIVERED)) {
            throw new InvalidStateTransitionException("Only delivered orders can be finalized");
        }
        status = OrderStatus.FINALIZED;
    }

    public void cancel() {
        if (!status.equals(OrderStatus.PENDING_PAYMENT)) {
            throw new InvalidStateTransitionException("Only pending payment orders can be cancelled");
        }
        status = OrderStatus.CANCELLED;
    }

    public void attachInvoice(Invoice invoice) {
        if (this.invoice != null) {
            throw new InvalidDataException("Order already has an invoice");
        }
        if (!status.equals(OrderStatus.PAID)) {
            throw new InvalidStateTransitionException("Invoice requires a paid order");
        }
        this.invoice = Objects.requireNonNull(invoice, "invoice cannot be null");
    }

    public void assertModifiable() {
        if (status.equals(OrderStatus.DELIVERED) || status.equals(OrderStatus.FINALIZED)) {
            throw new FinalizedOrderException("Finalized orders cannot be modified");
        }
        if (status.equals(OrderStatus.CANCELLED)) {
            throw new CancelledOrderException("Cancelled orders cannot be modified");
        }
    }

    public boolean requiresShipment() {
        return items.stream().anyMatch(item -> item.getProduct().requiresShipment());
    }

    public boolean isDigitalOnly() {
        return items.stream().noneMatch(item -> item.getProduct().requiresShipment());
    }

    public String getId() {
        return id;
    }

    public Buyer getBuyer() {
        return buyer;
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public List<StockAllocation> getAllocations() {
        return Collections.unmodifiableList(allocations);
    }

    public Address getDeliveryAddress() {
        return deliveryAddress;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
