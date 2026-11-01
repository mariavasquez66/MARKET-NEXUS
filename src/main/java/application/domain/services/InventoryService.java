package application.domain.services;

import application.domain.enums.AuditAction;
import application.domain.enums.AuditEntityType;
import application.domain.enums.MovementType;
import application.domain.enums.UserRole;
import application.domain.enums.WarehouseType;
import application.domain.exceptions.common.DuplicateEntityException;
import application.domain.exceptions.common.UnauthorizedOperationException;
import application.domain.exceptions.inventory.InventoryNotFoundException;
import application.domain.exceptions.product.InvalidProductTypeException;
import application.domain.exceptions.product.ProductNotFoundException;
import application.domain.exceptions.warehouse.InvalidWarehouseTypeException;
import application.domain.exceptions.warehouse.WarehouseNotFoundException;
import application.domain.models.Inventory;
import application.domain.models.InventoryMovement;
import application.domain.models.Product;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.InventoryMovementRepositoryPort;
import application.domain.ports.out.InventoryRepositoryPort;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.ports.out.WarehouseRepositoryPort;
import application.domain.valueobjects.StockAllocation;
import java.util.List;
import java.util.Objects;

public class InventoryService {
    private final InventoryRepositoryPort inventoryRepository;
    private final InventoryMovementRepositoryPort movementRepository;
    private final ProductRepositoryPort productRepository;
    private final WarehouseRepositoryPort warehouseRepository;
    private final AuditPort auditPort;
    private final ClockPort clockPort;

    public InventoryService(InventoryRepositoryPort inventoryRepository,
                            InventoryMovementRepositoryPort movementRepository,
                            ProductRepositoryPort productRepository,
                            WarehouseRepositoryPort warehouseRepository,
                            AuditPort auditPort,
                            ClockPort clockPort) {
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository);
        this.movementRepository = Objects.requireNonNull(movementRepository);
        this.productRepository = Objects.requireNonNull(productRepository);
        this.warehouseRepository = Objects.requireNonNull(warehouseRepository);
        this.auditPort = Objects.requireNonNull(auditPort);
        this.clockPort = Objects.requireNonNull(clockPort);
    }

    public Inventory registerInitialStock(User actor,
                                          String productId,
                                          String warehouseId,
                                          int quantity) {
        ServiceSupport.requireRole(actor, UserRole.SELLER);
        Product product = requireProduct(productId);
        if (!product.requiresInventory()) {
            throw new InvalidProductTypeException("Inventory is only allowed for physical products");
        }
        Warehouse warehouse = requireWarehouse(warehouseId);
        requireSellerOwns(actor, product);
        if (!warehouse.getType().equals(WarehouseType.SELLER)
                || warehouse.getOwner() == null
                || !warehouse.getOwner().getId().equals(product.getSeller().getId())) {
            throw new InvalidWarehouseTypeException("Warehouse must belong to the product seller");
        }
        if (inventoryRepository.findByProductIdAndWarehouseId(productId, warehouseId).isPresent()) {
            throw new DuplicateEntityException("Inventory already exists for product and warehouse");
        }
        Inventory inventory = inventoryRepository.save(Inventory.create(product, warehouse, quantity));
        if (quantity > 0) {
            recordMovement(inventory, MovementType.INFLOW, quantity, actor);
        }
        return inventory;
    }

    public Inventory addStock(User actor, String productId, String warehouseId, int quantity) {
        ServiceSupport.requireAnyRole(actor, UserRole.LOGISTIC_OPERATOR, UserRole.SELLER);
        if (quantity <= 0) {
            throw new application.domain.exceptions.cart.InvalidQuantityException("Quantity must be greater than zero");
        }
        Inventory inventory = requireInventory(productId, warehouseId);
        if (actor.hasRole(UserRole.SELLER)) {
            requireSellerOwns(actor, inventory.getProduct());
        }
        inventory.adjust(quantity);
        Inventory saved = inventoryRepository.save(inventory);
        recordMovement(saved, MovementType.INFLOW, quantity, actor);
        return saved;
    }

    public StockAllocation reserve(User actor, String productId, String warehouseId, int quantity) {
        ServiceSupport.requireAnyRole(actor, UserRole.BUYER, UserRole.ADMIN);
        Inventory inventory = requireInventory(productId, warehouseId);
        inventory.reserve(quantity);
        inventoryRepository.save(inventory);
        recordMovement(inventory, MovementType.RESERVATION, quantity, actor);
        return StockAllocation.of(productId, warehouseId, quantity);
    }

    public void releaseReservation(User actor, String productId, String warehouseId, int quantity) {
        ServiceSupport.requireAnyRole(actor, UserRole.BUYER, UserRole.ADMIN);
        Inventory inventory = requireInventory(productId, warehouseId);
        inventory.releaseReservation(quantity);
        inventoryRepository.save(inventory);
        recordMovement(inventory, MovementType.RESERVATION, quantity, actor);
    }

    public void confirmSale(User actor, String productId, String warehouseId, int quantity) {
        ServiceSupport.requireAnyRole(actor, UserRole.BUYER, UserRole.ADMIN);
        Inventory inventory = requireInventory(productId, warehouseId);
        inventory.confirmSale(quantity);
        inventoryRepository.save(inventory);
        recordMovement(inventory, MovementType.SALE, quantity, actor);
    }

    public Inventory adjust(User actor, String productId, String warehouseId, int delta) {
        ServiceSupport.requireRole(actor, UserRole.LOGISTIC_OPERATOR);
        Inventory inventory = requireInventory(productId, warehouseId);
        inventory.adjust(delta);
        Inventory saved = inventoryRepository.save(inventory);
        recordMovement(saved, MovementType.ADJUSTMENT, delta, actor);
        return saved;
    }

    public void registerReturn(User actor, String productId, String warehouseId, int quantity) {
        ServiceSupport.requireRole(actor, UserRole.LOGISTIC_OPERATOR);
        Inventory inventory = requireInventory(productId, warehouseId);
        inventory.registerReturn(quantity);
        inventoryRepository.save(inventory);
        recordMovement(inventory, MovementType.RETURN, quantity, actor);
    }

    public Inventory markDamaged(User actor, String productId, String warehouseId) {
        ServiceSupport.requireRole(actor, UserRole.LOGISTIC_OPERATOR);
        Inventory inventory = requireInventory(productId, warehouseId);
        inventory.markDamaged();
        return inventoryRepository.save(inventory);
    }

    public Inventory markRepaired(User actor, String productId, String warehouseId) {
        ServiceSupport.requireRole(actor, UserRole.LOGISTIC_OPERATOR);
        Inventory inventory = requireInventory(productId, warehouseId);
        inventory.markRepaired();
        return inventoryRepository.save(inventory);
    }

    public Inventory getAvailable(User actor, String productId, String warehouseId) {
        ServiceSupport.requireAnyRole(actor, UserRole.SELLER, UserRole.LOGISTIC_OPERATOR,
                UserRole.ADMIN, UserRole.SUPERVISOR);
        Inventory inventory = requireInventory(productId, warehouseId);
        if (actor.hasRole(UserRole.SELLER)) {
            requireSellerOwns(actor, inventory.getProduct());
        }
        return inventory;
    }

    public List<Inventory> listBySeller(User actor, String sellerId) {
        ServiceSupport.requireAnyRole(actor, UserRole.SELLER, UserRole.ADMIN, UserRole.SUPERVISOR);
        if (actor.hasRole(UserRole.SELLER)) {
            productRepository.findBySellerId(sellerId).stream().findFirst()
                    .ifPresent(product -> requireSellerOwns(actor, product));
        }
        return inventoryRepository.findBySellerId(sellerId);
    }

    public List<Inventory> listByProduct(User actor, String productId) {
        ServiceSupport.requireAnyRole(actor, UserRole.SELLER, UserRole.ADMIN, UserRole.SUPERVISOR);
        List<Inventory> inventories = inventoryRepository.findByProductId(productId);
        if (actor.hasRole(UserRole.SELLER) && !inventories.isEmpty()) {
            requireSellerOwns(actor, inventories.get(0).getProduct());
        }
        return inventories;
    }

    private void recordMovement(Inventory inventory, MovementType type, int quantity, User actor) {
        InventoryMovement movement = InventoryMovement.create(inventory, type, quantity, actor, clockPort.now());
        movementRepository.save(movement);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.INVENTORY_MOVEMENT,
                AuditEntityType.INVENTORY, inventory.getId(), actor,
                type.getCode() + " " + quantity);
    }

    private Inventory requireInventory(String productId, String warehouseId) {
        return inventoryRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found"));
    }

    private Product requireProduct(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productId));
    }

    private Warehouse requireWarehouse(String warehouseId) {
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new WarehouseNotFoundException("Warehouse not found: " + warehouseId));
    }

    private void requireSellerOwns(User actor, Product product) {
        if (!product.getSeller().getUser().getId().equals(actor.getId())) {
            throw new UnauthorizedOperationException("Seller does not own this product");
        }
    }
}
