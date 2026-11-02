package application.adapters.persistence.jpa.repositories;

import application.adapters.persistence.jpa.entities.InventoryJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryJpaRepository extends JpaRepository<InventoryJpaEntity, String> {

    Optional<InventoryJpaEntity> findByProductIdAndWarehouseId(String productId, String warehouseId);

    List<InventoryJpaEntity> findByProductId(String productId);

    List<InventoryJpaEntity> findByWarehouseId(String warehouseId);

    @Query("select i from InventoryJpaEntity i, WarehouseJpaEntity w "
            + "where i.warehouseId = w.id and w.ownerSellerId = :sellerId")
    List<InventoryJpaEntity> findByWarehouseOwnerSellerId(@Param("sellerId") String sellerId);

    boolean existsByProductId(String productId);
}
