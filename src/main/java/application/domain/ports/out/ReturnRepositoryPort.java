package application.domain.ports.out;

import application.domain.enums.ReturnStatus;
import application.domain.models.Return;
import java.util.List;
import java.util.Optional;

public interface ReturnRepositoryPort {
    Return save(Return returnRequest);

    Optional<Return> findById(String id);

    List<Return> findByOrderId(String orderId);

    List<Return> findByBuyerId(String buyerId);

    List<Return> findAll();

    List<Return> findByStatus(ReturnStatus status);
}
