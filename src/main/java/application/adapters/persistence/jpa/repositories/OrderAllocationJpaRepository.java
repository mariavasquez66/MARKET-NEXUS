package application.adapters.persistence.jpa.repositories;

import application.adapters.persistence.jpa.entities.OrderAllocationJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderAllocationJpaRepository extends JpaRepository<OrderAllocationJpaEntity, String> {

    List<OrderAllocationJpaEntity> findByOrderId(String orderId);
}
