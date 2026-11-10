package application.adapters.persistence.jpa;

import static application.domain.DomainFixtures.address;
import static org.assertj.core.api.Assertions.assertThat;

import application.adapters.persistence.jpa.adapters.InventoryJpaAdapter;
import application.adapters.persistence.jpa.adapters.JpaDependencyResolver;
import application.adapters.persistence.jpa.adapters.ProductJpaAdapter;
import application.adapters.persistence.jpa.adapters.SellerJpaAdapter;
import application.adapters.persistence.jpa.adapters.UserJpaAdapter;
import application.adapters.persistence.jpa.adapters.WarehouseJpaAdapter;
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
import application.domain.enums.AuditAction;
import application.domain.enums.ProductStatus;
import application.domain.enums.ProductType;
import application.domain.enums.UserRole;
import application.domain.models.AuditLog;
import application.domain.models.Inventory;
import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.InventoryRepositoryPort;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.ports.out.SellerRepositoryPort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.ports.out.WarehouseRepositoryPort;
import application.domain.services.CatalogService;
import application.domain.valueobjects.Email;
import application.domain.valueobjects.FullName;
import application.domain.valueobjects.IdentityDocument;
import application.domain.valueobjects.Money;
import application.domain.valueobjects.PasswordHash;
import application.domain.valueobjects.TaxId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@DataJpaTest
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
        UserJpaAdapter.class,
        SellerJpaAdapter.class,
        WarehouseJpaAdapter.class,
        ProductJpaAdapter.class,
        InventoryJpaAdapter.class,
        CatalogService.class,
        H2PhysicalProductPublishTest.NoOpPorts.class
})
class H2PhysicalProductPublishTest {

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private UserRepositoryPort userRepository;

    @Autowired
    private SellerRepositoryPort sellerRepository;

    @Autowired
    private WarehouseRepositoryPort warehouseRepository;

    @Autowired
    private ProductRepositoryPort productRepository;

    @Autowired
    private InventoryRepositoryPort inventoryRepository;

    @Test
    void publishesPersistedPhysicalProductAndListsItPublicly() {
        User admin = userRepository.save(user("Admin", UserRole.ADMIN, "admin@publish.test"));
        User sellerUser = userRepository.save(
                user("Seller", UserRole.SELLER, "seller@publish.test"));
        User operator = userRepository.save(
                user("Operator", UserRole.LOGISTIC_OPERATOR, "operator@publish.test"));

        Seller seller = sellerRepository.save(
                Seller.registerBy(admin, sellerUser, TaxId.of("901234567-8")));
        Warehouse warehouse = Warehouse.createSellerWarehouse(
                "Main warehouse", address(), seller, operator);
        seller.assignWarehouse(warehouse);
        warehouseRepository.save(warehouse);

        Product product = productRepository.save(Product.create(
                seller, "Laptop", "Persisted physical product", ProductType.PHYSICAL,
                Money.cop(new BigDecimal("119.00"))));
        inventoryRepository.save(Inventory.create(product, warehouse, 5));

        Product published = catalogService.publishProduct(sellerUser, product.getId());

        assertThat(published.getStatus()).isEqualTo(ProductStatus.PUBLISHED);
        Product stored = productRepository.findById(product.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(ProductStatus.PUBLISHED);
        assertThat(stored.getSeller().hasWarehouses()).isTrue();
        assertThat(productRepository.findPublished())
                .extracting(Product::getId)
                .contains(product.getId());
    }

    private static User user(String name, UserRole role, String email) {
        return User.create(
                FullName.of(name),
                Email.of(email),
                IdentityDocument.of("DOC" + role.getCode().replace("_", "") + email.hashCode()),
                role,
                PasswordHash.of("$2a$10$publishpersistence"));
    }

    @TestConfiguration
    static class NoOpPorts {

        @Bean
        AuditPort auditPort() {
            return new AuditPort() {
                @Override
                public void record(AuditLog auditLog) {
                }

                @Override
                public List<AuditLog> find(AuditAction action, LocalDate from, LocalDate to) {
                    return List.of();
                }

                @Override
                public List<AuditLog> findAll() {
                    return List.of();
                }
            };
        }

        @Bean
        ClockPort clockPort() {
            return new ClockPort() {
                @Override
                public Instant now() {
                    return Instant.parse("2026-01-01T00:00:00Z");
                }

                @Override
                public LocalDate today() {
                    return LocalDate.parse("2026-01-01");
                }
            };
        }
    }
}
