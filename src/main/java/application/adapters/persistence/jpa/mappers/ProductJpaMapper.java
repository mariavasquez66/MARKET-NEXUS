package application.adapters.persistence.jpa.mappers;

import application.adapters.persistence.jpa.entities.ProductJpaEntity;
import application.adapters.persistence.jpa.entities.ProductVariantJpaEntity;
import application.domain.enums.VariantType;
import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.valueobjects.Money;
import application.domain.valueobjects.ProductVariant;
import java.util.Currency;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProductJpaMapper {

    public ProductJpaEntity toEntity(Product product) {
        ProductJpaEntity entity = new ProductJpaEntity();
        entity.setId(product.getId());
        entity.setSellerId(product.getSeller().getId());
        entity.setName(product.getName());
        entity.setDescription(product.getDescription());
        entity.setTypeCode(product.getType());
        entity.setStatusCode(product.getStatus());
        entity.setPriceAmount(product.getPrice().getAmount());
        entity.setPriceCurrency(product.getPrice().getCurrency().getCurrencyCode());
        return entity;
    }

    public Product toDomain(ProductJpaEntity entity, Seller seller, List<ProductVariant> variants) {
        return Product.reconstitute(
                entity.getId(),
                seller,
                entity.getName(),
                entity.getDescription(),
                entity.getTypeCode(),
                entity.getStatusCode(),
                Money.of(entity.getPriceAmount(), Currency.getInstance(entity.getPriceCurrency())),
                variants);
    }

    public ProductVariantJpaEntity toEntity(ProductVariant variant, String productId) {
        ProductVariantJpaEntity entity = new ProductVariantJpaEntity();
        entity.setId(java.util.UUID.randomUUID().toString());
        entity.setProductId(productId);
        entity.setTypeCode(variant.getType().getCode());
        entity.setValue(variant.getValue());
        return entity;
    }

    public ProductVariant toDomain(ProductVariantJpaEntity entity) {
        return ProductVariant.of(VariantType.fromCode(entity.getTypeCode()), entity.getValue());
    }
}
