package application.adapters.persistence.jpa.repositories;

import application.adapters.persistence.jpa.entities.RefundJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefundJpaRepository extends JpaRepository<RefundJpaEntity, String> {

    List<RefundJpaEntity> findByReturnId(String returnId);

    @Query("select r from RefundJpaEntity r, ReturnJpaEntity ret "
            + "where r.returnId = ret.id and ret.orderId = :orderId")
    List<RefundJpaEntity> findByOrderId(@Param("orderId") String orderId);

    @Query("select r from RefundJpaEntity r, ReturnJpaEntity ret, OrderJpaEntity o "
            + "where r.returnId = ret.id and ret.orderId = o.id and o.buyerId = :buyerId")
    List<RefundJpaEntity> findByBuyerId(@Param("buyerId") String buyerId);
}
