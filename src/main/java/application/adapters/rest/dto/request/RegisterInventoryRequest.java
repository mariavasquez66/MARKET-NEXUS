package application.adapters.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record RegisterInventoryRequest(
        @NotBlank String productId,
        @NotBlank String warehouseId,
        @PositiveOrZero int initialQuantity) {
}
