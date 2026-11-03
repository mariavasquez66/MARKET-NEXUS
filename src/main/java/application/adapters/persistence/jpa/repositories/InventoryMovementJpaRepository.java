package application.adapters.persistence.jpa.repositories;

import application.adapters.persistence.jpa.entities.InventoryMovementJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementJpaRepository extends JpaRepository<InventoryMovementJpaEntity, String> {

    List<InventoryMovementJpaEntity> findByInventoryId(String inventoryId);
}
