package application.adapters.useCases;

import static application.domain.DomainFixtures.address;
import static application.domain.DomainFixtures.cartWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import application.adapters.persistence.jpa.adapters.BuyerJpaAdapter;
import application.adapters.persistence.jpa.adapters.CartJpaAdapter;
import application.adapters.persistence.jpa.adapters.InventoryJpaAdapter;
import application.adapters.persistence.jpa.adapters.InventoryMovementJpaAdapter;
import application.adapters.persistence.jpa.adapters.InvoiceJpaAdapter;
import application.adapters.persistence.jpa.adapters.JpaDependencyResolver;
import application.adapters.persistence.jpa.adapters.OrderJpaAdapter;
import application.adapters.persistence.jpa.adapters.ProductJpaAdapter;
import application.adapters.persistence.jpa.adapters.ReturnJpaAdapter;
import application.adapters.persistence.jpa.adapters.SellerJpaAdapter;
import application.adapters.persistence.jpa.adapters.ShipmentJpaAdapter;
import application.adapters.persistence.jpa.adapters.UserJpaAdapter;
import application.adapters.persistence.jpa.adapters.WarehouseJpaAdapter;
import application.adapters.persistence.jpa.mappers.BuyerJpaMapper;
import application.adapters.persistence.jpa.mappers.CartJpaMapper;
import application.adapters.persistence.jpa.mappers.InventoryJpaMapper;
import application.adapters.persistence.jpa.mappers.InventoryMovementJpaMapper;
import application.adapters.persistence.jpa.mappers.InvoiceJpaMapper;
import application.adapters.persistence.jpa.mappers.OrderJpaMapper;
import application.adapters.persistence.jpa.mappers.ProductJpaMapper;
import application.adapters.persistence.jpa.mappers.ReturnJpaMapper;
import application.adapters.persistence.jpa.mappers.SellerJpaMapper;
import application.adapters.persistence.jpa.mappers.ShipmentJpaMapper;
import application.adapters.persistence.jpa.mappers.UserJpaMapper;
import application.adapters.persistence.jpa.mappers.WarehouseJpaMapper;
import application.adapters.persistence.jpa.repositories.BuyerJpaRepository;
import application.adapters.persistence.jpa.repositories.UserJpaRepository;
import application.domain.enums.OrderStatus;
import application.domain.enums.ProductType;
import application.domain.enums.ReturnStatus;
import application.domain.enums.ShipmentStatus;
import application.domain.enums.UserRole;
import application.domain.exceptions.inventory.InventoryNotFoundException;
import application.domain.exceptions.order.InvalidStateTransitionException;
import application.domain.models.AuditLog;
import application.domain.models.Buyer;
import application.domain.models.Inventory;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.Return;
import application.domain.models.Seller;
import application.domain.models.Shipment;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.BuyerRepositoryPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.InventoryMovementRepositoryPort;
import application.domain.ports.out.InventoryRepositoryPort;
import application.domain.ports.out.JwtServicePort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.PasswordServicePort;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.ports.out.ReturnRepositoryPort;
import application.domain.ports.out.SellerRepositoryPort;
import application.domain.ports.out.ShipmentRepositoryPort;
import application.domain.ports.out.TaxPolicyPort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.ports.out.WarehouseRepositoryPort;
import application.domain.services.AuthService;
import application.domain.services.CatalogService;
import application.domain.services.InventoryService;
import application.domain.services.InvoiceService;
import application.domain.services.LogisticsService;
import application.domain.services.OrderService;
import application.domain.services.ReturnService;
import application.domain.valueobjects.Email;
import application.domain.valueobjects.FullName;
import application.domain.valueobjects.IdentityDocument;
import application.domain.valueobjects.Money;
import application.domain.valueobjects.PasswordHash;
import application.domain.valueobjects.StockAllocation;
import application.domain.valueobjects.TaxId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@Import({
        JpaDependencyResolver.class,
        UserJpaMapper.class,
        BuyerJpaMapper.class,
        SellerJpaMapper.class,
        WarehouseJpaMapper.class,
        ProductJpaMapper.class,
        InventoryJpaMapper.class,
        InventoryMovementJpaMapper.class,
        CartJpaMapper.class,
        OrderJpaMapper.class,
        InvoiceJpaMapper.class,
        ShipmentJpaMapper.class,
        ReturnJpaMapper.class,
        UserJpaAdapter.class,
        BuyerJpaAdapter.class,
        SellerJpaAdapter.class,
        WarehouseJpaAdapter.class,
        ProductJpaAdapter.class,
        InventoryJpaAdapter.class,
        InventoryMovementJpaAdapter.class,
        CartJpaAdapter.class,
        OrderJpaAdapter.class,
        InvoiceJpaAdapter.class,
        ShipmentJpaAdapter.class,
        ReturnJpaAdapter.class,
        AuthService.class,
        InvoiceService.class,
        InventoryService.class,
        LogisticsService.class,
        OrderService.class,
        ReturnService.class,
        PublicAccessUseCase.class,
        BusinessOperatorUseCase.class
})
class H2UseCaseRollbackTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private PublicAccessUseCase publicAccessUseCase;

    @Autowired
    private BusinessOperatorUseCase businessOperatorUseCase;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private BuyerJpaRepository buyerRepository;

    @Autowired
    private UserRepositoryPort users;

    @Autowired
    private BuyerRepositoryPort buyers;

    @Autowired
    private SellerRepositoryPort sellers;

    @Autowired
    private WarehouseRepositoryPort warehouses;

    @Autowired
    private ProductRepositoryPort products;

    @Autowired
    private InventoryRepositoryPort inventories;

    @Autowired
    private InventoryMovementRepositoryPort movements;

    @Autowired
    private OrderRepositoryPort orders;

    @Autowired
    private ShipmentRepositoryPort shipments;

    @Autowired
    private ReturnRepositoryPort returns;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private AuditPort auditPort;

    @MockitoBean
    private ClockPort clockPort;

    @MockitoBean
    private PasswordServicePort passwordService;

    @MockitoBean
    private JwtServicePort jwtService;

    @MockitoBean
    private CatalogService catalogService;

    @MockitoBean
    private TaxPolicyPort taxPolicy;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void registerBuyerRollsBackEveryWriteWhenAuditFails() {
        when(clockPort.now()).thenReturn(NOW);
        when(passwordService.encode(anyString())).thenReturn(PasswordHash.of("$2a$10$rollback"));
        doThrow(new IllegalStateException("audit unavailable"))
                .when(auditPort).record(any(AuditLog.class));

        Email email = Email.of("rolled-back@nexusmarket.test");
        IdentityDocument identityDocument = IdentityDocument.of("DOC-ROLLBACK-1");
        long buyersBefore = buyerRepository.count();

        assertThatThrownBy(() -> publicAccessUseCase.registerBuyer(
                FullName.of("Rollback Buyer"),
                email,
                identityDocument,
                "Password1",
                address()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("audit unavailable");

        assertThat(userRepository.existsByEmailValue(email.getValue())).isFalse();
        assertThat(userRepository.existsByIdentityDocumentValue(identityDocument.getValue()))
                .isFalse();
        assertThat(buyerRepository.count()).isEqualTo(buyersBefore);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void startShippingRollsBackShipmentWhenOrderDispatchFails() {
        when(clockPort.now()).thenReturn(NOW);

        User admin = users.save(user("Admin DEF04", UserRole.ADMIN,
                "admin-def04@nexusmarket.test"));
        User sellerUser = users.save(user("Seller DEF04", UserRole.SELLER,
                "seller-def04@nexusmarket.test"));
        User buyerUser = users.save(user("Buyer DEF04", UserRole.BUYER,
                "buyer-def04@nexusmarket.test"));
        User operator = users.save(user("Operator DEF04", UserRole.LOGISTIC_OPERATOR,
                "operator-def04@nexusmarket.test"));

        Seller seller = sellers.save(Seller.registerBy(admin, sellerUser, TaxId.of("910000001-1")));
        Warehouse warehouse = Warehouse.createSellerWarehouse(
                "DEF04 warehouse", address(), seller, operator);
        seller.assignWarehouse(warehouse);
        warehouses.save(warehouse);

        Product product = products.save(publishedProduct(seller, "DEF04 product"));
        Buyer buyer = buyers.save(Buyer.create(buyerUser, address()));

        Order order = Order.fromCart(cartWith(buyer, product, 1), address(), NOW);
        order.assignAllocations(List.of(
                StockAllocation.of(product.getId(), warehouse.getId(), 1)));
        orders.save(order);

        Shipment shipment = shipments.save(Shipment.create(order, warehouse));

        assertThatThrownBy(() -> businessOperatorUseCase.startShipping(
                operator, shipment.getId(), "TRACK-DEF04"))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("PAID");

        withinReadTransaction(() -> {
            Shipment stored = shipments.findById(shipment.getId()).orElseThrow();
            assertThat(stored.getStatus()).isEqualTo(ShipmentStatus.PENDING);
            assertThat(stored.getTrackingNumber()).isNull();
            assertThat(orders.findById(order.getId()).orElseThrow().getStatus())
                    .isEqualTo(OrderStatus.PENDING_PAYMENT);
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void receiveReturnRollsBackInventoryAndReturnWhenOneAllocationFails() {
        when(clockPort.now()).thenReturn(NOW);

        User admin = users.save(user("Admin DEF05", UserRole.ADMIN,
                "admin-def05@nexusmarket.test"));
        User sellerUser = users.save(user("Seller DEF05", UserRole.SELLER,
                "seller-def05@nexusmarket.test"));
        User buyerUser = users.save(user("Buyer DEF05", UserRole.BUYER,
                "buyer-def05@nexusmarket.test"));
        User operator = users.save(user("Operator DEF05", UserRole.LOGISTIC_OPERATOR,
                "operator-def05@nexusmarket.test"));

        Seller seller = sellers.save(Seller.registerBy(admin, sellerUser, TaxId.of("910000002-2")));
        Warehouse firstWarehouse = Warehouse.createSellerWarehouse(
                "DEF05 warehouse A", address(), seller, operator);
        Warehouse secondWarehouse = Warehouse.createSellerWarehouse(
                "DEF05 warehouse B", address(), seller, operator);
        seller.assignWarehouse(firstWarehouse);
        seller.assignWarehouse(secondWarehouse);
        warehouses.save(firstWarehouse);
        warehouses.save(secondWarehouse);

        Product firstProduct = products.save(publishedProduct(seller, "DEF05 product A"));
        Product secondProduct = products.save(publishedProduct(seller, "DEF05 product B"));
        Inventory firstInventory = inventories.save(
                Inventory.create(firstProduct, firstWarehouse, 10));

        Buyer buyer = buyers.save(Buyer.create(buyerUser, address()));
        Order order = Order.fromCart(cartWith(buyer, firstProduct, 1), address(), NOW);
        order.assignAllocations(List.of(
                StockAllocation.of(firstProduct.getId(), firstWarehouse.getId(), 1),
                StockAllocation.of(secondProduct.getId(), secondWarehouse.getId(), 1)));
        order.confirmPayment(NOW);
        order.markDispatched();
        order.markDelivered();
        orders.save(order);

        Return returnRequest = Return.request(order, "DEF05 damaged item", NOW);
        returnRequest.approve();
        returns.save(returnRequest);

        assertThatThrownBy(() -> businessOperatorUseCase.receiveReturn(
                operator, returnRequest.getId()))
                .isInstanceOf(InventoryNotFoundException.class);

        withinReadTransaction(() -> {
            Return storedReturn = returns.findById(returnRequest.getId()).orElseThrow();
            assertThat(storedReturn.getStatus()).isEqualTo(ReturnStatus.APPROVED);
            Inventory storedInventory = inventories.findById(firstInventory.getId()).orElseThrow();
            assertThat(storedInventory.getAvailableQuantity()).isEqualTo(10);
            assertThat(movements.findByInventoryId(firstInventory.getId())).isEmpty();
        });
    }

    private void withinReadTransaction(Runnable assertions) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> assertions.run());
    }

    private static User user(String name, UserRole role, String email) {
        return User.create(
                FullName.of(name),
                Email.of(email),
                IdentityDocument.of("DOC" + Integer.toHexString(email.hashCode()).toUpperCase()),
                role,
                PasswordHash.of("$2a$10$rollback"));
    }

    private static Product publishedProduct(Seller seller, String name) {
        Product product = Product.create(seller, name, name + " description",
                ProductType.PHYSICAL, Money.cop(new BigDecimal("119.00")));
        product.publish();
        return product;
    }
}
