package application.domain.ports.out;

import application.domain.models.Refund;
import java.util.List;
import java.util.Optional;

public interface RefundRepositoryPort {
    Refund save(Refund refund);

    Optional<Refund> findById(String id);

    List<Refund> findByReturnId(String returnId);

    List<Refund> findByOrderId(String orderId);

    List<Refund> findByBuyerId(String buyerId);

    List<Refund> findAll();
}
