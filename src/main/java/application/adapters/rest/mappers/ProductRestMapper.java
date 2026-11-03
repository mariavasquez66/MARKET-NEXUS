package application.adapters.rest.mappers;

import application.adapters.rest.dto.response.ProductResponse;
import application.adapters.rest.dto.response.ProductVariantResponse;
import application.domain.models.Product;
import application.domain.valueobjects.ProductVariant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProductRestMapper extends RestMapperSupport {

    public ProductResponse toResponse(Product product) {
        List<ProductVariantResponse> variants = product.getVariants().stream()
                .map(this::toVariant)
                .toList();
        return new ProductResponse(
                product.getId(),
                product.getSeller().getId(),
                product.getName(),
                product.getDescription(),
                product.getType().getCode(),
                product.getStatus().getCode(),
                toMoneyResponse(product.getPrice()),
                List.copyOf(variants));
    }

    public ProductVariantResponse toResponse(ProductVariant variant) {
        return toVariant(variant);
    }

    private ProductVariantResponse toVariant(ProductVariant variant) {
        return new ProductVariantResponse(variant.getType().getCode(), variant.getValue());
    }
}
