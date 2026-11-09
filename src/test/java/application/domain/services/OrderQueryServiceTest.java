package application.domain.services;

import static application.domain.DomainFixtures.address;
import static application.domain.DomainFixtures.admin;
import static application.domain.DomainFixtures.buyer;
import static application.domain.DomainFixtures.cartWith;
import static application.domain.DomainFixtures.operator;
import static application.domain.DomainFixtures.publishedPhysicalProduct;
import static application.domain.DomainFixtures.seller;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import application.domain.exceptions.common.UnauthorizedOperationException;
import application.domain.models.Buyer;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.ports.out.BuyerRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderQueryServiceTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock BuyerRepositoryPort buyerRepository;
    @Mock OrderRepositoryPort orderRepository;

    private OrderQueryService service;

    @BeforeEach
    void setUp() {
        service = new OrderQueryService(buyerRepository, orderRepository);
    }

    @Test
    void buyerCanOnlyReadOwnOrder() {
        Buyer buyer = buyer();
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Order order = Order.fromCart(cartWith(buyer, product, 1), address(), NOW);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThat(service.getOrder(buyer.getUser(), order.getId())).isSameAs(order);
        assertThatThrownBy(() -> service.getOrder(buyer().getUser(), order.getId()))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void buyerListsOnlyOwnOrders() {
        Buyer buyer = buyer();
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Order order = Order.fromCart(cartWith(buyer, product, 1), address(), NOW);
        when(buyerRepository.findByUserId(buyer.getUser().getId())).thenReturn(Optional.of(buyer));
        when(orderRepository.findByBuyerId(buyer.getId())).thenReturn(List.of(order));

        assertThat(service.listOrders(buyer.getUser())).containsExactly(order);
    }

    @Test
    void sellerListsOnlyOrdersContainingOwnProducts() {
        Seller seller = seller(admin());
        Product own = publishedPhysicalProduct(seller, operator());
        Buyer buyer = buyer();
        Order ownOrder = Order.fromCart(cartWith(buyer, own, 1), address(), NOW);
        Product foreign = publishedPhysicalProduct(seller(admin()), operator());
        Order foreignOrder = Order.fromCart(cartWith(buyer, foreign, 1), address(), NOW);
        when(orderRepository.findAll()).thenReturn(List.of(ownOrder, foreignOrder));

        assertThat(service.listOrders(seller.getUser())).containsExactly(ownOrder);
    }
}
