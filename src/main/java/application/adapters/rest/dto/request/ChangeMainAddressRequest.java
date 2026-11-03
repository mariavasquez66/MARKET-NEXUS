package application.adapters.rest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ChangeMainAddressRequest(
        @NotNull @Valid AddressRequest address) {
}
