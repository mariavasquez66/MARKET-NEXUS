package application.domain.models;

import static application.domain.DomainFixtures.admin;
import static application.domain.DomainFixtures.publishedDigitalProduct;
import static application.domain.DomainFixtures.publishedPhysicalProduct;
import static application.domain.DomainFixtures.operator;
import static application.domain.DomainFixtures.seller;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import application.domain.enums.ProductStatus;
import application.domain.enums.ProductType;
import application.domain.enums.VariantType;
import application.domain.exceptions.product.InvalidPriceException;
import application.domain.exceptions.product.InvalidProductStateException;
import application.domain.exceptions.seller.SellerWithoutWarehouseException;
import application.domain.valueobjects.Money;
import application.domain.valueobjects.ProductVariant;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductTest {

    @Test
    void physicalProductRequiresWarehouseToPublish() {
        Seller seller = seller(admin());
        Product product = Product.create(seller, "Laptop", "Desc", ProductType.PHYSICAL,
                Money.cop(new BigDecimal("10.00")));
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SUSPENDED);
        assertThatThrownBy(product::publish).isInstanceOf(SellerWithoutWarehouseException.class);
    }

    @Test
    void digitalProductPublishesWithoutWarehouse() {
        Product product = publishedDigitalProduct(seller(admin()));
        assertThat(product.getStatus()).isEqualTo(ProductStatus.PUBLISHED);
        assertThat(product.requiresShipment()).isFalse();
    }

    @Test
    void priceMustBePositive() {
        Seller seller = seller(admin());
        assertThatThrownBy(() -> Product.create(seller, "P", "D", ProductType.DIGITAL,
                Money.cop(BigDecimal.ZERO))).isInstanceOf(InvalidPriceException.class);
    }

    @Test
    void discontinuedProductCannotReturnToCatalog() {
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        product.discontinue();
        assertThatThrownBy(product::publish).isInstanceOf(InvalidProductStateException.class);
        assertThatThrownBy(product::suspend).isInstanceOf(InvalidProductStateException.class);
        assertThatThrownBy(() -> product.changeDetails("New", "New"))
                .isInstanceOf(InvalidProductStateException.class);
    }

    @Test
    void variantsCannotBeDuplicated() {
        Product product = publishedDigitalProduct(seller(admin()));
        product.addVariant(ProductVariant.of(VariantType.COLOR, "Red"));
        assertThatThrownBy(() -> product.addVariant(ProductVariant.of(VariantType.COLOR, "red")))
                .isInstanceOf(RuntimeException.class);
        assertThat(product.getVariants()).hasSize(1);
    }
}
