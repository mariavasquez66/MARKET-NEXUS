package application.adapters.useCases;

import static application.domain.DomainFixtures.address;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import application.adapters.persistence.jpa.adapters.BuyerJpaAdapter;
import application.adapters.persistence.jpa.adapters.CartJpaAdapter;
import application.adapters.persistence.jpa.adapters.InventoryJpaAdapter;
import application.adapters.persistence.jpa.adapters.InventoryMovementJpaAdapter;
import application.adapters.persistence.jpa.adapters.InvoiceJpaAdapter;
import application.adapters.persistence.jpa.adapters.JpaDependencyResolver;
import application.adapters.persistence.jpa.adapters.OrderJpaAdapter;
import application.adapters.persistence.jpa.adapters.ProductJpaAdapter;
import application.adapters.persistence.jpa.adapters.RefundJpaAdapter;
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
import application.adapters.persistence.jpa.mappers.RefundJpaMapper;
import application.adapters.persistence.jpa.mappers.ReturnJpaMapper;
import application.adapters.persistence.jpa.mappers.SellerJpaMapper;
import application.adapters.persistence.jpa.mappers.ShipmentJpaMapper;
import application.adapters.persistence.jpa.mappers.UserJpaMapper;
import application.adapters.persistence.jpa.mappers.WarehouseJpaMapper;
import application.domain.enums.OrderStatus;
import application.domain.enums.ProductType;
import application.domain.enums.UserRole;
import application.domain.models.Buyer;
import application.domain.models.Inventory;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.BuyerRepositoryPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.InventoryRepositoryPort;
import application.domain.ports.out.InvoiceRepositoryPort;
import application.domain.ports.out.JwtServicePort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.PasswordServicePort;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.ports.out.SellerRepositoryPort;
import application.domain.ports.out.TaxPolicyPort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.ports.out.WarehouseRepositoryPort;
import application.domain.services.AuthService;
import application.domain.services.BuyerService;
import application.domain.services.CartService;
import application.domain.services.CatalogService;
import application.domain.services.InventoryService;
import application.domain.services.InvoiceService;
import application.domain.services.LogisticsService;
import application.domain.services.OrderService;
import application.domain.services.RefundService;
import application.domain.services.ReportService;
import application.domain.services.ReturnService;
import application.domain.services.SellerService;
import application.domain.services.UserService;
import application.domain.services.WarehouseService;
import application.domain.valueobjects.Email;
import application.domain.valueobjects.FullName;
import application.domain.valueobjects.IdentityDocument;
import application.domain.valueobjects.Money;
import application.domain.valueobjects.PasswordHash;
import application.domain.valueobjects.TaxId;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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
        RefundJpaMapper.class,
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
        RefundJpaAdapter.class,
        AuthService.class,
        BuyerService.class,
        CartService.class,
        CatalogService.class,
        InventoryService.class,
        InvoiceService.class,
        LogisticsService.class,
        OrderService.class,
        RefundService.class,
        ReportService.class,
        ReturnService.class,
        SellerService.class,
        UserService.class,
        WarehouseService.class,
        NaturalCustomerUseCase.class
})
class H2PurchaseFlowTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private NaturalCustomerUseCase naturalCustomerUseCase;

    @Autowired
    private UserRepositoryPort userRepository;

    @Autowired
    private BuyerRepositoryPort buyerRepository;

    @Autowired
    private SellerRepositoryPort sellerRepository;

    @Autowired
    private WarehouseRepositoryPort warehouseRepository;

    @Autowired
    private ProductRepositoryPort productRepository;

    @Autowired
    private InventoryRepositoryPort inventoryRepository;

    @Autowired
    private OrderRepositoryPort orderRepository;

    @Autowired
    private InvoiceRepositoryPort invoiceRepository;

    @MockitoBean
    private AuditPort auditPort;

    @MockitoBean
    private ClockPort clockPort;

    @MockitoBean
    private PasswordServicePort passwordService;

    @MockitoBean
    private JwtServicePort jwtService;

    @MockitoBean
    private TaxPolicyPort taxPolicy;

    @Test
    void buyerCanPurchasePersistedPhysicalProductEndToEnd() {
        when(clockPort.now()).thenReturn(NOW);
        when(taxPolicy.currentTaxRate()).thenReturn(new BigDecimal("0.19"));

        User admin = userRepository.save(user("Admin", UserRole.ADMIN, "admin@endtoend.test"));
        User sellerUser = userRepository.save(user("Seller", UserRole.SELLER, "seller@endtoend.test"));
        User buyerUser = userRepository.save(user("Buyer", UserRole.BUYER, "buyer@endtoend.test"));
        User operator = userRepository.save(user("Operator", UserRole.LOGISTIC_OPERATOR,
                "operator@endtoend.test"));

        Seller seller = sellerRepository.save(
                Seller.registerBy(admin, sellerUser, TaxId.of("900123456-7")));
        Warehouse warehouse = Warehouse.createSellerWarehouse(
                "Main warehouse", address(), seller, operator);
        seller.assignWarehouse(warehouse);
        warehouseRepository.save(warehouse);

        Product product = Product.create(seller, "Laptop", "End-to-end laptop",
                ProductType.PHYSICAL, Money.cop(new BigDecimal("119.00")));
        product.publish();
        productRepository.save(product);

        Inventory inventory = inventoryRepository.save(
                Inventory.create(product, warehouse, 10));

        Buyer buyer = Buyer.create(buyerUser, address());
        buyerRepository.save(buyer);

        naturalCustomerUseCase.addCartItem(buyerUser, product.getId(), 2);
        Order checkout = naturalCustomerUseCase.checkout(buyerUser, address());

        assertThat(checkout.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(inventoryRepository.findById(inventory.getId()).orElseThrow()
                .getReservedQuantity()).isEqualTo(2);

        Order paid = naturalCustomerUseCase.payOrder(buyerUser, checkout.getId());

        assertThat(paid.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paid.getPaidAt()).isEqualTo(NOW);
        assertThat(paid.getInvoice()).isNotNull();
        assertThat(orderRepository.findById(paid.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.PAID);
        assertThat(invoiceRepository.findByOrderId(paid.getId())).isPresent();

        Inventory stored = inventoryRepository.findById(inventory.getId()).orElseThrow();
        assertThat(stored.getAvailableQuantity()).isEqualTo(8);
        assertThat(stored.getReservedQuantity()).isZero();
    }

    private static User user(String name, UserRole role, String email) {
        return User.create(
                FullName.of(name),
                Email.of(email),
                IdentityDocument.of("DOC" + role.getCode().replace("_", "") + "1"),
                role,
                PasswordHash.of("$2a$10$endtoend"));
    }
}
