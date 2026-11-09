package application.domain.services;

import application.domain.enums.OrderStatus;
import application.domain.models.Order;
import application.domain.models.User;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.BuyerRepositoryPort;
import application.domain.ports.out.CartRepositoryPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.InventoryMovementRepositoryPort;
import application.domain.ports.out.InventoryRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.ports.out.ShipmentRepositoryPort;
import application.domain.valueobjects.Address;
import java.util.List;
import java.util.Objects;

public class OrderService {
    private final CheckoutService checkoutService;
    private final OrderPaymentService paymentService;
    private final OrderFulfillmentService fulfillmentService;
    private final OrderLifecycleService lifecycleService;
    private final OrderQueryService queryService;

    public OrderService(CartRepositoryPort cartRepository,
                        BuyerRepositoryPort buyerRepository,
                        OrderRepositoryPort orderRepository,
                        ProductRepositoryPort productRepository,
                        InventoryRepositoryPort inventoryRepository,
                        InventoryMovementRepositoryPort movementRepository,
                        ShipmentRepositoryPort shipmentRepository,
                        InvoiceService invoiceService,
                        AuditPort auditPort,
                        ClockPort clockPort) {
        Objects.requireNonNull(cartRepository);
        Objects.requireNonNull(buyerRepository);
        Objects.requireNonNull(orderRepository);
        Objects.requireNonNull(productRepository);
        Objects.requireNonNull(inventoryRepository);
        Objects.requireNonNull(movementRepository);
        Objects.requireNonNull(shipmentRepository);
        Objects.requireNonNull(invoiceService);
        Objects.requireNonNull(auditPort);
        Objects.requireNonNull(clockPort);
        this.checkoutService = new CheckoutService(cartRepository, buyerRepository, orderRepository,
                productRepository, inventoryRepository, movementRepository, auditPort, clockPort);
        this.paymentService = new OrderPaymentService(orderRepository, inventoryRepository,
                movementRepository, invoiceService, auditPort, clockPort);
        this.fulfillmentService = new OrderFulfillmentService(orderRepository, shipmentRepository,
                auditPort, clockPort);
        this.lifecycleService = new OrderLifecycleService(orderRepository, inventoryRepository,
                movementRepository, auditPort, clockPort);
        this.queryService = new OrderQueryService(buyerRepository, orderRepository);
    }

    public Order checkout(User actor, Address deliveryAddress) {
        return checkoutService.checkout(actor, deliveryAddress);
    }

    public Order confirmPayment(User actor, String orderId) {
        return paymentService.confirmPayment(actor, orderId);
    }

    public Order cancel(User actor, String orderId) {
        return lifecycleService.cancel(actor, orderId);
    }

    public Order markDispatched(User actor, String orderId) {
        return fulfillmentService.markDispatched(actor, orderId);
    }

    public Order markDelivered(User actor, String orderId) {
        return fulfillmentService.markDelivered(actor, orderId);
    }

    public Order finalize(User actor, String orderId) {
        return lifecycleService.finalize(actor, orderId);
    }

    public Order getOrder(User actor, String orderId) {
        return queryService.getOrder(actor, orderId);
    }

    public List<Order> listOrders(User actor) {
        return queryService.listOrders(actor);
    }

    public List<Order> listByStatus(User actor, OrderStatus status) {
        return queryService.listByStatus(actor, status);
    }
}
