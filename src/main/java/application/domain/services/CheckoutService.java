package application.domain.services;

import application.domain.enums.AuditAction;
import application.domain.enums.AuditEntityType;
import application.domain.enums.MovementType;
import application.domain.enums.UserRole;
import application.domain.exceptions.cart.CartNotFoundException;
import application.domain.exceptions.cart.ProductNotAvailableException;
import application.domain.exceptions.inventory.InsufficientStockException;
import application.domain.exceptions.order.OrderNotFoundException;
import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.CartItem;
import application.domain.models.Inventory;
import application.domain.models.InventoryMovement;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.User;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.BuyerRepositoryPort;
import application.domain.ports.out.CartRepositoryPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.InventoryMovementRepositoryPort;
import application.domain.ports.out.InventoryRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.valueobjects.Address;
import application.domain.valueobjects.StockAllocation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class CheckoutService {
    private final CartRepositoryPort cartRepository;
    private final BuyerRepositoryPort buyerRepository;
    private final OrderRepositoryPort orderRepository;
    private final ProductRepositoryPort productRepository;
    private final InventoryRepositoryPort inventoryRepository;
    private final InventoryMovementRepositoryPort movementRepository;
    private final AuditPort auditPort;
    private final ClockPort clockPort;

    public CheckoutService(CartRepositoryPort cartRepository,
                           BuyerRepositoryPort buyerRepository,
                           OrderRepositoryPort orderRepository,
                           ProductRepositoryPort productRepository,
                           InventoryRepositoryPort inventoryRepository,
                           InventoryMovementRepositoryPort movementRepository,
                           AuditPort auditPort,
                           ClockPort clockPort) {
        this.cartRepository = Objects.requireNonNull(cartRepository);
        this.buyerRepository = Objects.requireNonNull(buyerRepository);
        this.orderRepository = Objects.requireNonNull(orderRepository);
        this.productRepository = Objects.requireNonNull(productRepository);
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository);
        this.movementRepository = Objects.requireNonNull(movementRepository);
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
                    throw new ProductNotAvailableException(
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

    private Buyer requireBuyer(User actor) {
        ServiceSupport.requireRole(actor, UserRole.BUYER);
        return buyerRepository.findByUserId(actor.getId())
                .orElseThrow(() -> new application.domain.exceptions.buyer.BuyerNotFoundException(
                        "Buyer not found for current user"));
    }
}
