package application.adapters.rest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record AddAddressRequest(
        @NotNull @Valid AddressRequest address) {
}
