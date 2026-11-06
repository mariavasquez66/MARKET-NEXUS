package application.domain.services;

import application.domain.enums.AuditAction;
import application.domain.enums.AuditEntityType;
import application.domain.enums.MovementType;
import application.domain.enums.UserRole;
import application.domain.exceptions.common.UnauthorizedOperationException;
import application.domain.exceptions.order.OrderNotFoundException;
import application.domain.models.Inventory;
import application.domain.models.InventoryMovement;
import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.User;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.InventoryMovementRepositoryPort;
import application.domain.ports.out.InventoryRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.valueobjects.StockAllocation;
import java.util.Objects;

public class OrderPaymentService {
    private final OrderRepositoryPort orderRepository;
    private final InventoryRepositoryPort inventoryRepository;
    private final InventoryMovementRepositoryPort movementRepository;
    private final InvoiceService invoiceService;
    private final AuditPort auditPort;
    private final ClockPort clockPort;

    public OrderPaymentService(OrderRepositoryPort orderRepository,
                               InventoryRepositoryPort inventoryRepository,
                               InventoryMovementRepositoryPort movementRepository,
                               InvoiceService invoiceService,
                               AuditPort auditPort,
                               ClockPort clockPort) {
        this.orderRepository = Objects.requireNonNull(orderRepository);
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository);
        this.movementRepository = Objects.requireNonNull(movementRepository);
        this.invoiceService = Objects.requireNonNull(invoiceService);
        this.auditPort = Objects.requireNonNull(auditPort);
        this.clockPort = Objects.requireNonNull(clockPort);
    }

    public Order confirmPayment(User actor, String orderId) {
        Order order = requireOrder(orderId);
        requireBuyerOwner(actor, order);
        order.confirmPayment(clockPort.now());
        for (StockAllocation allocation : order.getAllocations()) {
            Inventory inventory = inventoryRepository
                    .findByProductIdAndWarehouseId(allocation.getProductId(), allocation.getWarehouseId())
                    .orElseThrow(() -> new application.domain.exceptions.inventory.InventoryNotFoundException(
                            "Inventory not found for allocation"));
            inventory.confirmSale(allocation.getQuantity());
            inventoryRepository.save(inventory);
            movementRepository.save(InventoryMovement.create(inventory, MovementType.SALE,
                    allocation.getQuantity(), actor, clockPort.now()));
        }
        Invoice invoice = invoiceService.generateInvoice(actor, order.getId());
        order.attachInvoice(invoice);
        String details = "PENDING_PAYMENT -> PAID";
        if (order.isDigitalOnly()) {
            order.markDelivered();
            details = "PENDING_PAYMENT -> PAID -> DELIVERED";
        }
        Order saved = orderRepository.save(order);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.ORDER_STATUS_CHANGED,
                AuditEntityType.ORDER, saved.getId(), actor, details);
        return saved;
    }

    private void requireBuyerOwner(User actor, Order order) {
        ServiceSupport.requireRole(actor, UserRole.BUYER);
        if (!order.getBuyer().getUser().getId().equals(actor.getId())) {
            throw new UnauthorizedOperationException("Order does not belong to current buyer");
        }
    }

    private Order requireOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }
}
