package application.domain.services;

import application.domain.enums.AuditAction;
import application.domain.enums.AuditEntityType;
import application.domain.enums.ShipmentStatus;
import application.domain.enums.UserRole;
import application.domain.exceptions.order.InvalidStateTransitionException;
import application.domain.exceptions.order.OrderNotFoundException;
import application.domain.models.Order;
import application.domain.models.Shipment;
import application.domain.models.User;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.ShipmentRepositoryPort;
import application.domain.valueobjects.StockAllocation;
import java.util.List;
import java.util.Objects;

public class OrderFulfillmentService {
    private final OrderRepositoryPort orderRepository;
    private final ShipmentRepositoryPort shipmentRepository;
    private final AuditPort auditPort;
    private final ClockPort clockPort;

    public OrderFulfillmentService(OrderRepositoryPort orderRepository,
                                   ShipmentRepositoryPort shipmentRepository,
                                   AuditPort auditPort,
                                   ClockPort clockPort) {
        this.orderRepository = Objects.requireNonNull(orderRepository);
        this.shipmentRepository = Objects.requireNonNull(shipmentRepository);
        this.auditPort = Objects.requireNonNull(auditPort);
        this.clockPort = Objects.requireNonNull(clockPort);
    }

    public Order markDispatched(User actor, String orderId) {
        ServiceSupport.requireAnyRole(actor, UserRole.LOGISTIC_OPERATOR, UserRole.ADMIN);
        Order order = requireOrder(orderId);
        if (order.getAllocations().isEmpty()) {
            throw new InvalidStateTransitionException("Order has no shipments to dispatch");
        }
        for (StockAllocation allocation : order.getAllocations()) {
            List<Shipment> shipments = shipmentRepository
                    .findByOrderIdAndOriginWarehouseId(orderId, allocation.getWarehouseId());
            boolean allInTransit = !shipments.isEmpty() && shipments.stream().allMatch(shipment ->
                    shipment.getStatus().equals(ShipmentStatus.IN_TRANSIT)
                            || shipment.getStatus().equals(ShipmentStatus.DELIVERED));
            if (!allInTransit) {
                throw new InvalidStateTransitionException("Not all shipments are in transit");
            }
        }
        order.markDispatched();
        Order saved = orderRepository.save(order);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.ORDER_STATUS_CHANGED,
                AuditEntityType.ORDER, saved.getId(), actor, "PAID -> DISPATCHED");
        return saved;
    }

    public Order markDelivered(User actor, String orderId) {
        ServiceSupport.requireAnyRole(actor, UserRole.LOGISTIC_OPERATOR, UserRole.ADMIN);
        Order order = requireOrder(orderId);
        if (order.getAllocations().isEmpty()) {
            throw new InvalidStateTransitionException("Order has no shipments to deliver");
        }
        for (StockAllocation allocation : order.getAllocations()) {
            List<Shipment> shipments = shipmentRepository
                    .findByOrderIdAndOriginWarehouseId(orderId, allocation.getWarehouseId());
            boolean allDelivered = !shipments.isEmpty()
                    && shipments.stream().allMatch(shipment ->
                            shipment.getStatus().equals(ShipmentStatus.DELIVERED));
            if (!allDelivered) {
                throw new InvalidStateTransitionException("Not all shipments are delivered");
            }
        }
        order.markDelivered();
        Order saved = orderRepository.save(order);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.ORDER_STATUS_CHANGED,
                AuditEntityType.ORDER, saved.getId(), actor, "DISPATCHED -> DELIVERED");
        return saved;
    }

    private Order requireOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }
}
