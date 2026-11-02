package application.adapters.persistence.jpa.repositories;

import application.adapters.persistence.jpa.entities.ReturnJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReturnJpaRepository extends JpaRepository<ReturnJpaEntity, String> {

    List<ReturnJpaEntity> findByOrderId(String orderId);

    List<ReturnJpaEntity> findByStatusCode(String statusCode);

    @Query("select r from ReturnJpaEntity r, OrderJpaEntity o "
            + "where r.orderId = o.id and o.buyerId = :buyerId")
    List<ReturnJpaEntity> findByBuyerId(@Param("buyerId") String buyerId);
}
