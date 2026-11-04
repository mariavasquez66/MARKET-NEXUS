package application.adapters.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AddressRequest(
        @NotBlank String street,
        @NotBlank String city,
        @NotBlank String region,
        String postalCode,
        @NotBlank String country) {
}
