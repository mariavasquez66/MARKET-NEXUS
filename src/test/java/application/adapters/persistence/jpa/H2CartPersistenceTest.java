package application.adapters.persistence.jpa;

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
import application.adapters.useCases.NaturalCustomerUseCase;
import application.domain.enums.ProductType;
import application.domain.enums.UserRole;
import application.domain.models.Buyer;
import application.domain.models.Cart;
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
import application.domain.ports.out.JwtServicePort;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
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
class H2CartPersistenceTest {
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
    void keepsSuccessiveItemsAndClearsTheCartAfterCheckout() {
        when(clockPort.now()).thenReturn(NOW);
        when(taxPolicy.currentTaxRate()).thenReturn(new BigDecimal("0.19"));

        User admin = userRepository.save(user("Admin", UserRole.ADMIN, "admin@cart.test", "CART-ADMIN"));
        User sellerUser = userRepository.save(
                user("Seller", UserRole.SELLER, "seller@cart.test", "CART-SELLER"));
        User buyerUser = userRepository.save(
                user("Buyer", UserRole.BUYER, "buyer@cart.test", "CART-BUYER"));
        User operator = userRepository.save(
                user("Operator", UserRole.LOGISTIC_OPERATOR, "operator@cart.test", "CART-OPERATOR"));

        Seller seller = sellerRepository.save(
                Seller.registerBy(admin, sellerUser, TaxId.of("901234567-8")));
        Warehouse warehouse = Warehouse.createSellerWarehouse(
                "Cart warehouse", address(), seller, operator);
        seller.assignWarehouse(warehouse);
        warehouseRepository.save(warehouse);

        Product first = product(seller, "First product", "10.00");
        Product second = product(seller, "Second product", "20.00");
        Product third = product(seller, "Third product", "30.00");
        productRepository.save(first);
        productRepository.save(second);
        productRepository.save(third);
        inventoryRepository.save(Inventory.create(first, warehouse, 10));
        inventoryRepository.save(Inventory.create(second, warehouse, 10));
        inventoryRepository.save(Inventory.create(third, warehouse, 10));

        Buyer buyer = buyerRepository.save(Buyer.create(buyerUser, address()));

        naturalCustomerUseCase.addCartItem(buyerUser, first.getId(), 1);
        naturalCustomerUseCase.addCartItem(buyerUser, second.getId(), 1);
        naturalCustomerUseCase.addCartItem(buyerUser, third.getId(), 1);

        Cart stored = naturalCustomerUseCase.getCart(buyerUser);
        assertThat(stored.getItems()).hasSize(3);
        assertThat(stored.getBuyer().getId()).isEqualTo(buyer.getId());
        assertThat(stored.total().getAmount()).isEqualByComparingTo("60.00");

        naturalCustomerUseCase.addCartItem(buyerUser, first.getId(), 1);

        Cart updated = naturalCustomerUseCase.getCart(buyerUser);
        assertThat(updated.getItems()).hasSize(3);
        assertThat(updated.getItems()).anySatisfy(item -> {
            assertThat(item.getProduct().getId()).isEqualTo(first.getId());
            assertThat(item.getQuantity()).isEqualTo(2);
        });
        assertThat(updated.total().getAmount()).isEqualByComparingTo("70.00");

        naturalCustomerUseCase.removeCartItem(buyerUser, second.getId());

        Cart afterRemoval = naturalCustomerUseCase.getCart(buyerUser);
        assertThat(afterRemoval.getItems()).hasSize(2);
        assertThat(afterRemoval.total().getAmount()).isEqualByComparingTo("50.00");

        Order order = naturalCustomerUseCase.checkout(buyerUser, address());

        assertThat(order.getItems()).hasSize(2);
        assertThat(naturalCustomerUseCase.getCart(buyerUser).getItems()).isEmpty();
    }

    private static Product product(Seller seller, String name, String price) {
        Product product = Product.create(seller, name, name + " description",
                ProductType.PHYSICAL, Money.cop(new BigDecimal(price)));
        product.publish();
        return product;
    }

    private static User user(String name, UserRole role, String email, String document) {
        return User.create(
                FullName.of(name),
                Email.of(email),
                IdentityDocument.of(document),
                role,
                PasswordHash.of("$2a$10$cartpersistence"));
    }
}
