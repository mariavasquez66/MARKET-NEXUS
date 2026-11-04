package application.adapters.rest.dto.response;

import java.util.List;

public record ProductResponse(
        String id,
        String sellerId,
        String name,
        String description,
        String type,
        String status,
        MoneyResponse price,
        List<ProductVariantResponse> variants) {
}
