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
import application.domain.enums.ReturnStatus;
import application.domain.enums.UserRole;
import application.domain.models.Buyer;
import application.domain.models.Cart;
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
import java.util.ArrayList;
import java.util.List;
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
class H2TeacherScenarioTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private NaturalCustomerUseCase naturalCustomerUseCase;

    @Autowired
    private LogisticsService logisticsService;

    @Autowired
    private ReturnService returnService;

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
    void buyerPurchasesFromThreeSellersAndReturnsOneProduct() {
        when(clockPort.now()).thenReturn(NOW);
        when(taxPolicy.currentTaxRate()).thenReturn(new BigDecimal("0.19"));

        User admin = userRepository.save(user("Admin", UserRole.ADMIN, "admin@teacher.test"));
        User operator = userRepository.save(user("Operator", UserRole.LOGISTIC_OPERATOR,
                "operator@teacher.test"));
        User buyerUser = userRepository.save(user("Buyer", UserRole.BUYER, "buyer@teacher.test"));

        List<ScenarioProduct> scenario = new ArrayList<>();
        scenario.add(persistProduct(admin, operator, 1, "Auriculares", "120.00", 1));
        scenario.add(persistProduct(admin, operator, 2, "Teclado", "250.00", 2));
        scenario.add(persistProduct(admin, operator, 3, "Libro", "80.00", 1));

        Buyer buyer = buyerRepository.save(Buyer.create(buyerUser, address()));
        Cart cart = null;
        for (ScenarioProduct current : scenario) {
            cart = naturalCustomerUseCase.addCartItem(buyerUser, current.product().getId(),
                    current.quantity());
        }

        assertThat(cart.getItems()).hasSize(3);
        assertThat(cart.getItems().stream().map(item -> item.getProduct().getSeller().getId())
                .distinct()).hasSize(3);
        assertThat(cart.total().getAmount()).isEqualByComparingTo(new BigDecimal("700.00"));

        Order order = naturalCustomerUseCase.checkout(buyerUser, address());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.getAllocations()).hasSize(3);
        assertThat(naturalCustomerUseCase.getCart(buyerUser).getItems()).isEmpty();
        for (ScenarioProduct current : scenario) {
            Inventory stored = inventoryRepository.findById(current.inventory().getId()).orElseThrow();
            assertThat(stored.getReservedQuantity()).isEqualTo(current.quantity());
        }

        Order paid = naturalCustomerUseCase.payOrder(buyerUser, order.getId());

        assertThat(paid.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paid.getInvoice()).isNotNull();
        assertThat(invoiceRepository.findByOrderId(paid.getId())).isPresent();
        for (ScenarioProduct current : scenario) {
            Inventory stored = inventoryRepository.findById(current.inventory().getId()).orElseThrow();
            assertThat(stored.getAvailableQuantity()).isEqualTo(10 - current.quantity());
            assertThat(stored.getReservedQuantity()).isZero();
        }

        List<Shipment> shipments = new ArrayList<>();
        for (ScenarioProduct current : scenario) {
            shipments.add(logisticsService.createShipment(operator, paid.getId(),
                    current.warehouse().getId()));
        }
        int tracking = 0;
        for (Shipment shipment : shipments) {
            logisticsService.startShipping(operator, shipment.getId(), "TRK-" + (++tracking));
        }
        assertThat(orderRepository.findById(paid.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.DISPATCHED);

        for (Shipment shipment : shipments) {
            logisticsService.confirmDelivery(operator, shipment.getId());
        }
        assertThat(orderRepository.findById(paid.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.DELIVERED);

        ScenarioProduct returnedProduct = scenario.get(2);
        Return requested = returnService.requestReturn(buyerUser, paid.getId(),
                "Producto defectuoso", returnedProduct.product().getId(), 1);
        assertThat(requested.getStatus()).isEqualTo(ReturnStatus.REQUESTED);
        assertThat(requested.getProductId()).isEqualTo(returnedProduct.product().getId());
        assertThat(requested.getQuantity()).isEqualTo(1);

        Return approved = returnService.approve(admin, requested.getId());
        assertThat(approved.getStatus()).isEqualTo(ReturnStatus.APPROVED);

        Return completed = returnService.complete(operator, approved.getId());
        assertThat(completed.getStatus()).isEqualTo(ReturnStatus.COMPLETED);

        for (ScenarioProduct current : scenario) {
            Inventory stored = inventoryRepository.findById(current.inventory().getId()).orElseThrow();
            int expectedAvailable = current == returnedProduct
                    ? 10
                    : 10 - current.quantity();
            assertThat(stored.getAvailableQuantity()).isEqualTo(expectedAvailable);
            assertThat(stored.getReservedQuantity()).isZero();
        }
    }

    private ScenarioProduct persistProduct(User admin,
                                           User operator,
                                           int index,
                                           String name,
                                           String price,
                                           int quantity) {
        User sellerUser = userRepository.save(user("Seller " + index, UserRole.SELLER,
                "seller" + index + "@teacher.test"));
        Seller seller = sellerRepository.save(Seller.registerBy(admin, sellerUser,
                TaxId.of("90012345" + index)));
        Warehouse warehouse = Warehouse.createSellerWarehouse("Warehouse " + index, address(),
                seller, operator);
        seller.assignWarehouse(warehouse);
        warehouseRepository.save(warehouse);

        Product product = Product.create(seller, name, "Product " + index,
                ProductType.PHYSICAL, Money.cop(new BigDecimal(price)));
        product.publish();
        productRepository.save(product);

        Inventory inventory = inventoryRepository.save(Inventory.create(product, warehouse, 10));
        return new ScenarioProduct(product, warehouse, inventory, quantity);
    }

    private record ScenarioProduct(Product product,
                                   Warehouse warehouse,
                                   Inventory inventory,
                                   int quantity) {
    }

    private static User user(String name, UserRole role, String email) {
        return User.create(
                FullName.of(name),
                Email.of(email),
                IdentityDocument.of("DOC" + role.getCode().replace("_", "") + email.hashCode()),
                role,
                PasswordHash.of("$2a$10$teacher"));
    }
}
