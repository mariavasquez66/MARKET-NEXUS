package application.adapters.persistence.jpa.repositories;

import application.adapters.persistence.jpa.entities.InvoiceJpaEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceJpaRepository extends JpaRepository<InvoiceJpaEntity, String> {

    Optional<InvoiceJpaEntity> findByOrderId(String orderId);

    List<InvoiceJpaEntity> findByOrderIdIn(Collection<String> orderIds);

    boolean existsByOrderId(String orderId);

    @Query("select i from InvoiceJpaEntity i, OrderJpaEntity o "
            + "where i.orderId = o.id and o.buyerId = :buyerId")
    List<InvoiceJpaEntity> findByBuyerId(@Param("buyerId") String buyerId);
}
