package application.domain.ports.out;

import application.domain.enums.OrderStatus;
import application.domain.enums.PaymentStatus;
import application.domain.models.Order;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepositoryPort {
    Order save(Order order);

    Optional<Order> findById(String id);

    List<Order> findAll();

    List<Order> findByBuyerId(String buyerId);

    List<Order> findBySellerId(String sellerId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByBuyerIdAndStatus(String buyerId, OrderStatus status);

    List<Order> findByPaymentStatusAndCreatedAtBetween(
            PaymentStatus paymentStatus, Instant from, Instant to);
}
