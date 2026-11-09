package application.adapters.persistence.jpa;

import static application.domain.DomainFixtures.address;
import static application.domain.DomainFixtures.admin;
import static application.domain.DomainFixtures.buyer;
import static application.domain.DomainFixtures.cartWith;
import static application.domain.DomainFixtures.operator;
import static application.domain.DomainFixtures.publishedPhysicalProduct;
import static application.domain.DomainFixtures.seller;
import static org.assertj.core.api.Assertions.assertThat;

import application.adapters.persistence.jpa.entities.OrderJpaEntity;
import application.adapters.persistence.jpa.mappers.OrderJpaMapper;
import application.adapters.persistence.jpa.repositories.OrderJpaRepository;
import application.domain.models.Order;
import application.domain.models.Product;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
class H2OrderPersistenceTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private OrderJpaRepository orderRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final OrderJpaMapper mapper = new OrderJpaMapper();

    @Test
    void persistsPaidOrderWithItsPaymentInstant() {
        Order order = paidOrder();

        orderRepository.saveAndFlush(mapper.toEntity(order));
        OrderJpaEntity stored = orderRepository.findById(order.getId()).orElseThrow();

        assertThat(stored.getStatusCode()).isEqualTo(order.getStatus());
        assertThat(stored.getPaymentStatusCode()).isEqualTo(order.getPaymentStatus());
        assertThat(stored.getPaidAt()).isEqualTo(order.getPaidAt());
        assertThat(stored.getVersion()).isNotNull();
    }

    @Test
    void rollsBackTheOrderWhenTheTransactionIsMarkedRollbackOnly() {
        Order order = paidOrder();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        transaction.execute(status -> {
            orderRepository.saveAndFlush(mapper.toEntity(order));
            status.setRollbackOnly();
            return null;
        });

        assertThat(orderRepository.findById(order.getId())).isEmpty();
    }

    private static Order paidOrder() {
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Order order = Order.fromCart(cartWith(buyer(), product, 1), address(), NOW);
        order.confirmPayment(NOW.plusSeconds(30));
        return order;
    }
}
