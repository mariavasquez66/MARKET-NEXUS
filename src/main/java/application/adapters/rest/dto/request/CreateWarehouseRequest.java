package application.adapters.rest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateWarehouseRequest(
        @NotBlank String name,
        @NotNull @Valid AddressRequest location,
        @NotBlank String type,
        String ownerSellerId,
        @NotBlank String responsibleUserId) {
}
