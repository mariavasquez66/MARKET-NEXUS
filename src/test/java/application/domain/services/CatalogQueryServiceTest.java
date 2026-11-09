package application.domain.services;

import static application.domain.DomainFixtures.admin;
import static application.domain.DomainFixtures.operator;
import static application.domain.DomainFixtures.publishedPhysicalProduct;
import static application.domain.DomainFixtures.seller;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import application.domain.exceptions.common.UnauthorizedOperationException;
import application.domain.exceptions.product.ProductNotPublishedException;
import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.ports.out.SellerRepositoryPort;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogQueryServiceTest {

    @Mock ProductRepositoryPort productRepository;
    @Mock SellerRepositoryPort sellerRepository;
    @InjectMocks CatalogQueryService service;

    @Test
    void getPublishedProductRejectsNonPurchasableProduct() {
        Seller seller = seller(admin());
        Product product = Product.create(seller, "Laptop", "Laptop description",
                application.domain.enums.ProductType.PHYSICAL,
                application.domain.valueobjects.Money.cop(java.math.BigDecimal.ONE));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.getPublishedProduct(product.getId()))
                .isInstanceOf(ProductNotPublishedException.class);
    }

    @Test
    void sellerCanOnlyReadOwnProduct() {
        Seller owner = seller(admin());
        Product product = publishedPhysicalProduct(owner, operator());
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        assertThat(service.getProductForSeller(owner.getUser(), product.getId())).isSameAs(product);
        assertThatThrownBy(() -> service.getProductForSeller(
                seller(admin()).getUser(), product.getId()))
                .isInstanceOf(UnauthorizedOperationException.class);
    }
}
