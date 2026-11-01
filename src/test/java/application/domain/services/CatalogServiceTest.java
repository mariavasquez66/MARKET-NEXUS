package application.domain.services;

import static application.domain.DomainFixtures.admin;
import static application.domain.DomainFixtures.operator;
import static application.domain.DomainFixtures.publishedDigitalProduct;
import static application.domain.DomainFixtures.publishedPhysicalProduct;
import static application.domain.DomainFixtures.seller;
import static application.domain.DomainFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import application.domain.enums.ProductType;
import application.domain.enums.UserRole;
import application.domain.exceptions.common.UnauthorizedOperationException;
import application.domain.exceptions.product.InvalidProductStateException;
import application.domain.exceptions.product.ProductNotPublishedException;
import application.domain.exceptions.seller.SellerWithoutWarehouseException;
import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.InventoryRepositoryPort;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.ports.out.SellerRepositoryPort;
import application.domain.valueobjects.Money;
import java.math.BigDecimal;
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
class CatalogServiceTest {
    @Mock ProductRepositoryPort productRepository;
    @Mock SellerRepositoryPort sellerRepository;
    @Mock InventoryRepositoryPort inventoryRepository;
    @Mock AuditPort auditPort;
    @Mock ClockPort clockPort;

    private CatalogService service;

    @BeforeEach
    void setUp() {
        service = new CatalogService(productRepository, sellerRepository, inventoryRepository,
                auditPort, clockPort);
        when(clockPort.now()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void physicalProductRequiresWarehouseBeforeRegistration() {
        Seller seller = seller(admin());
        when(sellerRepository.findByUserId(seller.getUser().getId())).thenReturn(Optional.of(seller));
        assertThatThrownBy(() -> service.registerProduct(seller.getUser(), "Laptop", "Desc",
                ProductType.PHYSICAL, Money.cop(new BigDecimal("10"))))
                .isInstanceOf(SellerWithoutWarehouseException.class);
    }

    @Test
    void registerProductRequiresSellerRole() {
        assertThatThrownBy(() -> service.registerProduct(admin(), "P", "D",
                ProductType.DIGITAL, Money.cop(BigDecimal.TEN)))
                .isInstanceOf(application.domain.exceptions.common.InsufficientPermissionException.class);
    }

    @Test
    void publishPhysicalProductRequiresInventory() {
        Seller seller = seller(admin());
        Product product = Product.create(seller, "Laptop", "Desc", ProductType.PHYSICAL,
                Money.cop(new BigDecimal("10")));
        seller.assignWarehouse(application.domain.models.Warehouse.createSellerWarehouse(
                "W", application.domain.DomainFixtures.address(), seller, operator()));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(inventoryRepository.existsByProductId(product.getId())).thenReturn(false);
        assertThatThrownBy(() -> service.publishProduct(seller.getUser(), product.getId()))
                .isInstanceOf(InvalidProductStateException.class);
    }

    @Test
    void sellerCannotManageAnotherSellersProduct() {
        Seller owner = seller(admin());
        Product product = publishedDigitalProduct(owner);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        Seller other = seller(admin());
        assertThatThrownBy(() -> service.updateProduct(other.getUser(), product.getId(), "N", "D",
                Money.cop(BigDecimal.TEN))).isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void publicCatalogOnlyReturnsPublishedProducts() {
        when(productRepository.findPublished()).thenReturn(List.of());
        assertThat(service.listPublishedProducts()).isEmpty();
        Product product = publishedDigitalProduct(seller(admin()));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        assertThat(service.getPublishedProduct(product.getId())).isSameAs(product);
        product.suspend();
        assertThatThrownBy(() -> service.getPublishedProduct(product.getId()))
                .isInstanceOf(ProductNotPublishedException.class);
    }

    @Test
    void adminCanViewAnyProductButSellerOnlyOwn() {
        Seller owner = seller(admin());
        Product product = publishedPhysicalProduct(owner, operator());
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        assertThat(service.getProductForSeller(admin(), product.getId())).isSameAs(product);
        assertThatThrownBy(() -> service.getProductForSeller(user(UserRole.SELLER), product.getId()))
                .isInstanceOf(UnauthorizedOperationException.class);
    }
}
