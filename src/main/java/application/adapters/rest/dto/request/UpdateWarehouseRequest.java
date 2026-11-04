package application.adapters.rest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateWarehouseRequest(
        @NotBlank String name,
        @NotNull @Valid AddressRequest location,
        @NotBlank String responsibleUserId) {
}
