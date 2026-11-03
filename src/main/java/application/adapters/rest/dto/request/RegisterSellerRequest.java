package application.adapters.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RegisterSellerRequest(
        @NotBlank String userId,
        @NotBlank String taxId) {
}
