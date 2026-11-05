package application.domain.services;

import application.domain.enums.AuditAction;
import application.domain.enums.AuditEntityType;
import application.domain.enums.MovementType;
import application.domain.enums.OrderStatus;
import application.domain.enums.ShipmentStatus;
import application.domain.enums.UserRole;
import application.domain.exceptions.cart.CartNotFoundException;
import application.domain.exceptions.inventory.InsufficientStockException;
import application.domain.exceptions.order.InvalidStateTransitionException;
import application.domain.exceptions.order.OrderNotFoundException;
import application.domain.exceptions.common.UnauthorizedOperationException;
import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.CartItem;
import application.domain.models.Inventory;
import application.domain.models.InventoryMovement;
import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.Shipment;
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
import application.domain.valueobjects.Money;
import application.domain.valueobjects.StockAllocation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class OrderService {
    private final CartRepositoryPort cartRepository;
    private final BuyerRepositoryPort buyerRepository;
    private final OrderRepositoryPort orderRepository;
    private final ProductRepositoryPort productRepository;
    private final InventoryRepositoryPort inventoryRepository;
    private final InventoryMovementRepositoryPort movementRepository;
    private final ShipmentRepositoryPort shipmentRepository;
    private final InvoiceService invoiceService;
    private final AuditPort auditPort;
    private final ClockPort clockPort;

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
        this.cartRepository = Objects.requireNonNull(cartRepository);
        this.buyerRepository = Objects.requireNonNull(buyerRepository);
        this.orderRepository = Objects.requireNonNull(orderRepository);
        this.productRepository = Objects.requireNonNull(productRepository);
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository);
        this.movementRepository = Objects.requireNonNull(movementRepository);
        this.shipmentRepository = Objects.requireNonNull(shipmentRepository);
        this.invoiceService = Objects.requireNonNull(invoiceService);
        this.auditPort = Objects.requireNonNull(auditPort);
        this.clockPort = Objects.requireNonNull(clockPort);
    }

    public Order checkout(User actor, Address deliveryAddress) {
        Buyer buyer = requireBuyer(actor);
        Cart cart = cartRepository.findByBuyerId(buyer.getId())
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));
        cart.assertNotEmpty();
        buyer.assertCanPurchase();
        List<StockAllocation> allocations = new ArrayList<>();
        List<Inventory> reserved = new ArrayList<>();
        try {
            for (CartItem item : cart.getItems()) {
                Product product = item.getProduct();
                if (!product.isPurchasable()) {
                    throw new application.domain.exceptions.cart.ProductNotAvailableException(
                            "Product is not available: " + product.getId());
                }
                if (product.requiresInventory()) {
                    reserveAcrossWarehouses(actor, product, item.getQuantity(), allocations, reserved);
                }
            }
            Order order = Order.fromCart(cart, deliveryAddress, clockPort.now());
            if (!allocations.isEmpty()) {
                order.assignAllocations(allocations);
            }
            Order saved = orderRepository.save(order);
            cart.clear();
            cartRepository.save(cart);
            ServiceSupport.audit(auditPort, clockPort, AuditAction.ORDER_CREATED,
                    AuditEntityType.ORDER, saved.getId(), actor, "Order created");
            return saved;
        } catch (RuntimeException exception) {
            releaseReservations(reserved, allocations);
            throw exception;
        }
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

    public Order cancel(User actor, String orderId) {
        Order order = requireOrder(orderId);
        requireBuyerOwner(actor, order);
        order.cancel();
        for (StockAllocation allocation : order.getAllocations()) {
            Inventory inventory = inventoryRepository
                    .findByProductIdAndWarehouseId(allocation.getProductId(), allocation.getWarehouseId())
                    .orElseThrow(() -> new application.domain.exceptions.inventory.InventoryNotFoundException(
                            "Inventory not found for allocation"));
            inventory.releaseReservation(allocation.getQuantity());
            inventoryRepository.save(inventory);
            movementRepository.save(InventoryMovement.create(inventory, MovementType.RESERVATION,
                    allocation.getQuantity(), actor, clockPort.now()));
        }
        Order saved = orderRepository.save(order);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.ORDER_STATUS_CHANGED,
                AuditEntityType.ORDER, saved.getId(), actor, "PENDING_PAYMENT -> CANCELLED");
        return saved;
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

    public Order finalize(User actor, String orderId) {
        ServiceSupport.requireAnyRole(actor, UserRole.BUYER, UserRole.ADMIN);
        Order order = requireOrder(orderId);
        if (actor.hasRole(UserRole.BUYER)) {
            requireBuyerOwner(actor, order);
        }
        order.finalizeOrder();
        Order saved = orderRepository.save(order);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.ORDER_STATUS_CHANGED,
                AuditEntityType.ORDER, saved.getId(), actor, "DELIVERED -> FINALIZED");
        return saved;
    }

    public Order getOrder(User actor, String orderId) {
        ServiceSupport.requireAnyRole(actor, UserRole.BUYER, UserRole.SELLER,
                UserRole.LOGISTIC_OPERATOR, UserRole.SUPERVISOR, UserRole.ADMIN);
        Order order = requireOrder(orderId);
        if (canView(actor, order)) {
            return order;
        }
        throw new UnauthorizedOperationException("Cannot access this order");
    }

    public List<Order> listOrders(User actor) {
        return listOrders(actor, null);
    }

    public List<Order> listByStatus(User actor, OrderStatus status) {
        Objects.requireNonNull(status, "status cannot be null");
        return listOrders(actor, status);
    }

    private List<Order> listOrders(User actor, OrderStatus status) {
        ServiceSupport.requireAnyRole(actor, UserRole.BUYER, UserRole.SELLER,
                UserRole.LOGISTIC_OPERATOR, UserRole.SUPERVISOR, UserRole.ADMIN);
        if (actor.hasRole(UserRole.BUYER)) {
            Buyer buyer = buyerRepository.findByUserId(actor.getId())
                    .orElseThrow(() -> new application.domain.exceptions.buyer.BuyerNotFoundException(
                            "Buyer not found for current user"));
            return status == null
                    ? orderRepository.findByBuyerId(buyer.getId())
                    : orderRepository.findByBuyerIdAndStatus(buyer.getId(), status);
        }
        List<Order> orders = status == null ? orderRepository.findAll() : orderRepository.findByStatus(status);
        if (actor.hasRole(UserRole.SELLER)) {
            return orders.stream().filter(order -> belongsToSeller(order, actor.getId())).toList();
        }
        return orders;
    }

    private void reserveAcrossWarehouses(User actor,
                                         Product product,
                                         int quantity,
                                         List<StockAllocation> allocations,
                                         List<Inventory> reserved) {
        List<Inventory> candidates = inventoryRepository.findByProductId(product.getId()).stream()
                .filter(inventory -> inventory.getAvailableQuantity() > 0)
                .sorted(Comparator.comparingInt(Inventory::getAvailableQuantity).reversed())
                .toList();
        int remaining = quantity;
        for (Inventory inventory : candidates) {
            if (remaining == 0) {
                break;
            }
            int take = Math.min(remaining, inventory.getAvailableQuantity());
            inventory.reserve(take);
            Inventory saved = inventoryRepository.save(inventory);
            reserved.add(saved);
            allocations.add(StockAllocation.of(product.getId(), saved.getWarehouse().getId(), take));
            remaining -= take;
        }
        if (remaining > 0) {
            releaseReservations(reserved, allocations);
            reserved.clear();
            allocations.clear();
            throw new InsufficientStockException("Insufficient stock for product: " + product.getId());
        }
    }

    private void releaseReservations(List<Inventory> reserved, List<StockAllocation> allocations) {
        for (Inventory inventory : reserved) {
            Inventory current = inventoryRepository.findById(inventory.getId()).orElse(inventory);
            int quantity = allocations.stream()
                    .filter(allocation -> allocation.getWarehouseId()
                            .equals(inventory.getWarehouse().getId())
                            && allocation.getProductId().equals(inventory.getProduct().getId()))
                    .mapToInt(StockAllocation::getQuantity)
                    .sum();
            if (quantity > 0) {
                current.releaseReservation(quantity);
                inventoryRepository.save(current);
            }
        }
    }

    private boolean canView(User actor, Order order) {
        if (actor.hasRole(UserRole.ADMIN) || actor.hasRole(UserRole.SUPERVISOR)
                || actor.hasRole(UserRole.LOGISTIC_OPERATOR)) {
            return true;
        }
        if (actor.hasRole(UserRole.BUYER)) {
            return order.getBuyer().getUser().getId().equals(actor.getId());
        }
        return belongsToSeller(order, actor.getId());
    }

    private boolean belongsToSeller(Order order, String userId) {
        return order.getItems().stream()
                .anyMatch(item -> item.getProduct().getSeller().getUser().getId().equals(userId));
    }

    private void requireBuyerOwner(User actor, Order order) {
        ServiceSupport.requireRole(actor, UserRole.BUYER);
        if (!order.getBuyer().getUser().getId().equals(actor.getId())) {
            throw new UnauthorizedOperationException("Order does not belong to current buyer");
        }
    }

    private Buyer requireBuyer(User actor) {
        ServiceSupport.requireRole(actor, UserRole.BUYER);
        return buyerRepository.findByUserId(actor.getId())
                .orElseThrow(() -> new application.domain.exceptions.buyer.BuyerNotFoundException(
                        "Buyer not found for current user"));
    }

    private Order requireOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }
}
