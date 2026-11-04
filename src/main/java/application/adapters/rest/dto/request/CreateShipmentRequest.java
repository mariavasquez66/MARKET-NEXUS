package application.adapters.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateShipmentRequest(
        @NotBlank String orderId,
        @NotBlank String warehouseId) {
}
