package application.adapters.rest.dto.response;

import java.util.List;

public record SellerResponse(
        String id,
        String userId,
        String fullName,
        String email,
        String taxId,
        List<String> warehouseIds) {
}
