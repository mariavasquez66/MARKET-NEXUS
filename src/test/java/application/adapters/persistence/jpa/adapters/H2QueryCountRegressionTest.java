package application.adapters.persistence.jpa.adapters;

import static org.assertj.core.api.Assertions.assertThat;

import application.adapters.persistence.jpa.entities.AddressEmbeddable;
import application.adapters.persistence.jpa.entities.BuyerJpaEntity;
import application.adapters.persistence.jpa.entities.OrderItemJpaEntity;
import application.adapters.persistence.jpa.entities.OrderJpaEntity;
import application.adapters.persistence.jpa.entities.ProductJpaEntity;
import application.adapters.persistence.jpa.entities.SellerJpaEntity;
import application.adapters.persistence.jpa.entities.UserJpaEntity;
import application.adapters.persistence.jpa.mappers.BuyerJpaMapper;
import application.adapters.persistence.jpa.mappers.CartJpaMapper;
import application.adapters.persistence.jpa.mappers.InventoryJpaMapper;
import application.adapters.persistence.jpa.mappers.InvoiceJpaMapper;
import application.adapters.persistence.jpa.mappers.OrderJpaMapper;
import application.adapters.persistence.jpa.mappers.ProductJpaMapper;
import application.adapters.persistence.jpa.mappers.ReturnJpaMapper;
import application.adapters.persistence.jpa.mappers.SellerJpaMapper;
import application.adapters.persistence.jpa.mappers.UserJpaMapper;
import application.adapters.persistence.jpa.mappers.WarehouseJpaMapper;
import application.adapters.persistence.jpa.repositories.BuyerJpaRepository;
import application.adapters.persistence.jpa.repositories.OrderItemJpaRepository;
import application.adapters.persistence.jpa.repositories.OrderJpaRepository;
import application.adapters.persistence.jpa.repositories.ProductJpaRepository;
import application.adapters.persistence.jpa.repositories.SellerJpaRepository;
import application.adapters.persistence.jpa.repositories.UserJpaRepository;
import application.domain.enums.CommercialStatus;
import application.domain.enums.OrderStatus;
import application.domain.enums.PaymentStatus;
import application.domain.enums.ProductStatus;
import application.domain.enums.ProductType;
import application.domain.enums.UserRole;
import application.domain.enums.UserStatus;
import application.domain.ports.out.OrderRepositoryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import({
        JpaDependencyResolver.class,
        UserJpaMapper.class,
        BuyerJpaMapper.class,
        SellerJpaMapper.class,
        WarehouseJpaMapper.class,
        ProductJpaMapper.class,
        InventoryJpaMapper.class,
        CartJpaMapper.class,
        OrderJpaMapper.class,
        InvoiceJpaMapper.class,
        ReturnJpaMapper.class,
        OrderJpaAdapter.class
})
class H2QueryCountRegressionTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private OrderRepositoryPort orderRepository;

    @Autowired
    private OrderJpaRepository orderEntities;

    @Autowired
    private OrderItemJpaRepository orderItemEntities;

    @Autowired
    private UserJpaRepository userEntities;

    @Autowired
    private BuyerJpaRepository buyerEntities;

    @Autowired
    private SellerJpaRepository sellerEntities;

    @Autowired
    private ProductJpaRepository productEntities;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void listingOrdersByStatusDoesNotGrowQueriesWithOrderCount() {
        String buyerId = seedBuyer();
        String productId = seedProduct();

        addOrders(buyerId, productId, 5);
        flushAndClear();
        long fiveOrders = countQueries(() -> orderRepository.findByStatus(OrderStatus.PAID));

        addOrders(buyerId, productId, 45);
        flushAndClear();
        long fiftyOrders = countQueries(() -> orderRepository.findByStatus(OrderStatus.PAID));

        assertThat(fiveOrders).isPositive();
        assertThat(fiftyOrders).isEqualTo(fiveOrders);
    }

    private long countQueries(Runnable runnable) {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        runnable.run();
        return statistics.getPrepareStatementCount();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private String seedBuyer() {
        UserJpaEntity user = user(UserRole.BUYER, "buyer@query.test", "QUERY-BUYER");
        userEntities.save(user);

        BuyerJpaEntity buyer = new BuyerJpaEntity();
        buyer.setId(UUID.randomUUID().toString());
        buyer.setUserId(user.getId());
        buyer.setMainAddress(address());
        buyer.setCommercialStatusCode(CommercialStatus.ELIGIBLE);
        buyerEntities.save(buyer);
        return buyer.getId();
    }

    private String seedProduct() {
        UserJpaEntity user = user(UserRole.SELLER, "seller@query.test", "QUERY-SELLER");
        userEntities.save(user);

        SellerJpaEntity seller = new SellerJpaEntity();
        seller.setId(UUID.randomUUID().toString());
        seller.setUserId(user.getId());
        seller.setTaxIdValue("900123456-7");
        sellerEntities.save(seller);

        ProductJpaEntity product = new ProductJpaEntity();
        product.setId(UUID.randomUUID().toString());
        product.setSellerId(seller.getId());
        product.setName("Query product");
        product.setDescription("Query product description");
        product.setTypeCode(ProductType.PHYSICAL);
        product.setStatusCode(ProductStatus.PUBLISHED);
        product.setPriceAmount(new BigDecimal("119.00"));
        product.setPriceCurrency("COP");
        productEntities.save(product);
        return product.getId();
    }

    private void addOrders(String buyerId, String productId, int quantity) {
        for (int index = 0; index < quantity; index++) {
            OrderJpaEntity order = new OrderJpaEntity();
            order.setId(UUID.randomUUID().toString());
            order.setBuyerId(buyerId);
            order.setStatusCode(OrderStatus.PAID);
            order.setTotalAmount(new BigDecimal("119.00"));
            order.setTotalCurrency("COP");
            order.setPaymentStatusCode(PaymentStatus.CONFIRMED);
            order.setDeliveryAddress(address());
            order.setCreatedAt(NOW);
            order.setPaidAt(NOW);
            orderEntities.save(order);

            OrderItemJpaEntity item = new OrderItemJpaEntity();
            item.setId(UUID.randomUUID().toString());
            item.setOrderId(order.getId());
            item.setProductId(productId);
            item.setQuantity(1);
            item.setUnitPriceAmount(new BigDecimal("119.00"));
            item.setUnitPriceCurrency("COP");
            orderItemEntities.save(item);
        }
    }

    private static UserJpaEntity user(UserRole role, String email, String document) {
        UserJpaEntity user = new UserJpaEntity();
        user.setId(UUID.randomUUID().toString());
        user.setFullNameValue("Query user");
        user.setEmailValue(email);
        user.setIdentityDocumentValue(document);
        user.setRoleCode(role);
        user.setStatusCode(UserStatus.ACTIVE);
        user.setPasswordHashValue("$2a$10$query");
        user.setAuthTokenVersion(0);
        return user;
    }

    private static AddressEmbeddable address() {
        AddressEmbeddable address = new AddressEmbeddable();
        address.setStreet("Calle 1 # 2-3");
        address.setCity("Bogota");
        address.setRegion("Cundinamarca");
        address.setPostalCode("110111");
        address.setCountry("Colombia");
        return address;
    }
}
