package application.adapters.persistence.jpa.repositories;

import application.adapters.persistence.jpa.entities.OrderItemJpaEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemJpaRepository extends JpaRepository<OrderItemJpaEntity, String> {

    List<OrderItemJpaEntity> findByOrderId(String orderId);

    List<OrderItemJpaEntity> findByOrderIdIn(Collection<String> orderIds);
}
