package application.domain.services;

import application.domain.enums.UserRole;
import application.domain.exceptions.common.UnauthorizedOperationException;
import application.domain.exceptions.inventory.InventoryNotFoundException;
import application.domain.models.Inventory;
import application.domain.models.Product;
import application.domain.models.User;
import application.domain.ports.out.InventoryRepositoryPort;
import java.util.List;
import java.util.Objects;

public class InventoryQueryService {
    private final InventoryRepositoryPort inventoryRepository;

    public InventoryQueryService(InventoryRepositoryPort inventoryRepository) {
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository);
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
        List<Inventory> inventories = inventoryRepository.findBySellerId(sellerId);
        requireSellerOwnsAll(actor, inventories);
        return inventories;
    }

    public List<Inventory> listByProduct(User actor, String productId) {
        ServiceSupport.requireAnyRole(actor, UserRole.SELLER, UserRole.ADMIN, UserRole.SUPERVISOR);
        List<Inventory> inventories = inventoryRepository.findByProductId(productId);
        requireSellerOwnsAll(actor, inventories);
        return inventories;
    }

    public List<Inventory> listMyInventory(User actor) {
        ServiceSupport.requireRole(actor, UserRole.SELLER);
        return inventoryRepository.findAll().stream()
                .filter(inventory -> inventory.getProduct().getSeller().getUser().getId()
                        .equals(actor.getId()))
                .toList();
    }

    private Inventory requireInventory(String productId, String warehouseId) {
        return inventoryRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found"));
    }

    private void requireSellerOwns(User actor, Product product) {
        if (!product.getSeller().getUser().getId().equals(actor.getId())) {
            throw new UnauthorizedOperationException("Seller does not own this product");
        }
    }

    private void requireSellerOwnsAll(User actor, List<Inventory> inventories) {
        if (actor.hasRole(UserRole.SELLER)
                && inventories.stream().anyMatch(inventory ->
                        !inventory.getProduct().getSeller().getUser().getId().equals(actor.getId()))) {
            throw new UnauthorizedOperationException("Seller does not own all requested inventory");
        }
    }
}
