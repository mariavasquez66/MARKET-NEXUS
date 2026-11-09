package application.adapters.persistence.jpa.repositories;

import application.adapters.persistence.jpa.entities.OrderJpaEntity;
import application.domain.enums.OrderStatus;
import application.domain.enums.PaymentStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, String> {

    List<OrderJpaEntity> findByBuyerId(String buyerId);

    List<OrderJpaEntity> findByStatusCode(OrderStatus statusCode);

    List<OrderJpaEntity> findByBuyerIdAndStatusCode(String buyerId, OrderStatus statusCode);

    List<OrderJpaEntity> findByPaymentStatusCodeAndCreatedAtBetween(
            PaymentStatus paymentStatusCode, Instant from, Instant to);

    @Query("select distinct o from OrderJpaEntity o, OrderItemJpaEntity i, ProductJpaEntity p "
            + "where i.orderId = o.id and i.productId = p.id and p.sellerId = :sellerId")
    List<OrderJpaEntity> findDistinctBySellerId(@Param("sellerId") String sellerId);
}
