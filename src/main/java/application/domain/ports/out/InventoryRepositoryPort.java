package application.domain.ports.out;

import application.domain.models.Inventory;
import java.util.List;
import java.util.Optional;

public interface InventoryRepositoryPort {
    Inventory save(Inventory inventory);

    Optional<Inventory> findById(String id);

    Optional<Inventory> findByProductIdAndWarehouseId(String productId, String warehouseId);

    List<Inventory> findByProductId(String productId);

    List<Inventory> findByWarehouseId(String warehouseId);

    List<Inventory> findBySellerId(String sellerId);

    List<Inventory> findAll();

    boolean existsByProductId(String productId);
}
