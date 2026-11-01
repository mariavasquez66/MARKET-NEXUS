package application.domain.services;

import application.domain.enums.AuditAction;
import application.domain.enums.AuditEntityType;
import application.domain.enums.ShipmentStatus;
import application.domain.enums.UserRole;
import application.domain.exceptions.common.DuplicateEntityException;
import application.domain.exceptions.common.InvalidDataException;
import application.domain.exceptions.common.UnauthorizedOperationException;
import application.domain.exceptions.logistics.ShipmentNotFoundException;
import application.domain.exceptions.order.OrderNotFoundException;
import application.domain.exceptions.product.InvalidProductTypeException;
import application.domain.exceptions.warehouse.InvalidWarehouseTypeException;
import application.domain.exceptions.warehouse.WarehouseNotFoundException;
import application.domain.models.Order;
import application.domain.models.Shipment;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.ShipmentRepositoryPort;
import application.domain.ports.out.WarehouseRepositoryPort;
import application.domain.valueobjects.StockAllocation;
import java.util.List;
import java.util.Objects;

public class LogisticsService {
    private final ShipmentRepositoryPort shipmentRepository;
    private final OrderRepositoryPort orderRepository;
    private final WarehouseRepositoryPort warehouseRepository;
    private final OrderService orderService;
    private final AuditPort auditPort;
    private final ClockPort clockPort;

    public LogisticsService(ShipmentRepositoryPort shipmentRepository,
                            OrderRepositoryPort orderRepository,
                            WarehouseRepositoryPort warehouseRepository,
                            OrderService orderService,
                            AuditPort auditPort,
                            ClockPort clockPort) {
        this.shipmentRepository = Objects.requireNonNull(shipmentRepository);
        this.orderRepository = Objects.requireNonNull(orderRepository);
        this.warehouseRepository = Objects.requireNonNull(warehouseRepository);
        this.orderService = Objects.requireNonNull(orderService);
        this.auditPort = Objects.requireNonNull(auditPort);
        this.clockPort = Objects.requireNonNull(clockPort);
    }

    public Shipment createShipment(User actor, String orderId, String warehouseId) {
        ServiceSupport.requireRole(actor, UserRole.LOGISTIC_OPERATOR);
        Order order = requireOrder(orderId);
        if (!order.requiresShipment()) {
            throw new InvalidProductTypeException("Digital orders do not require shipment");
        }
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new WarehouseNotFoundException("Warehouse not found: " + warehouseId));
        boolean assigned = order.getAllocations().stream()
                .map(StockAllocation::getWarehouseId)
                .anyMatch(id -> id.equals(warehouseId));
        if (!assigned) {
            throw new InvalidWarehouseTypeException("Warehouse is not assigned to this order");
        }
        if (!shipmentRepository.findByOrderIdAndOriginWarehouseId(orderId, warehouseId).isEmpty()) {
            throw new DuplicateEntityException("Shipment already exists for order and warehouse");
        }
        Shipment saved = shipmentRepository.save(Shipment.create(order, warehouse));
        ServiceSupport.audit(auditPort, clockPort, AuditAction.SHIPMENT_CREATED,
                AuditEntityType.SHIPMENT, saved.getId(), actor, "Shipment created");
        return saved;
    }

    public Shipment startShipping(User actor, String shipmentId, String trackingNumber) {
        ServiceSupport.requireRole(actor, UserRole.LOGISTIC_OPERATOR);
        Shipment shipment = requireShipment(shipmentId);
        shipment.startShipping(trackingNumber);
        Shipment saved = shipmentRepository.save(shipment);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.SHIPMENT_STARTED,
                AuditEntityType.SHIPMENT, saved.getId(), actor, "Shipment started");
        if (allShipmentsInTransit(saved.getOrder().getId())) {
            orderService.markDispatched(actor, saved.getOrder().getId());
        }
        return saved;
    }

    public Shipment confirmDelivery(User actor, String shipmentId) {
        ServiceSupport.requireRole(actor, UserRole.LOGISTIC_OPERATOR);
        Shipment shipment = requireShipment(shipmentId);
        shipment.confirmDelivery();
        Shipment saved = shipmentRepository.save(shipment);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.SHIPMENT_DELIVERED,
                AuditEntityType.SHIPMENT, saved.getId(), actor, "Shipment delivered");
        if (allShipmentsDelivered(saved.getOrder().getId())) {
            orderService.markDelivered(actor, saved.getOrder().getId());
        }
        return saved;
    }

    public List<Shipment> listByOrder(User actor, String orderId) {
        ServiceSupport.requireAnyRole(actor, UserRole.LOGISTIC_OPERATOR, UserRole.SUPERVISOR,
                UserRole.ADMIN, UserRole.BUYER);
        Order order = requireOrder(orderId);
        if (actor.hasRole(UserRole.BUYER)
                && !order.getBuyer().getUser().getId().equals(actor.getId())) {
            throw new UnauthorizedOperationException("Cannot access another buyer shipments");
        }
        return shipmentRepository.findByOrderId(orderId);
    }

    public Shipment getShipment(User actor, String shipmentId) {
        ServiceSupport.requireAnyRole(actor, UserRole.LOGISTIC_OPERATOR, UserRole.SUPERVISOR,
                UserRole.ADMIN, UserRole.BUYER);
        Shipment shipment = requireShipment(shipmentId);
        if (actor.hasRole(UserRole.BUYER)
                && !shipment.getOrder().getBuyer().getUser().getId().equals(actor.getId())) {
            throw new UnauthorizedOperationException("Cannot access another buyer shipment");
        }
        return shipment;
    }

    private boolean allShipmentsInTransit(String orderId) {
        List<Shipment> shipments = shipmentRepository.findByOrderId(orderId);
        return !shipments.isEmpty() && shipments.stream().allMatch(shipment ->
                shipment.getStatus().equals(ShipmentStatus.IN_TRANSIT)
                        || shipment.getStatus().equals(ShipmentStatus.DELIVERED));
    }

    private boolean allShipmentsDelivered(String orderId) {
        List<Shipment> shipments = shipmentRepository.findByOrderId(orderId);
        return !shipments.isEmpty()
                && shipments.stream().allMatch(shipment -> shipment.getStatus().equals(ShipmentStatus.DELIVERED));
    }

    private Shipment requireShipment(String shipmentId) {
        return shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found: " + shipmentId));
    }

    private Order requireOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }
}
