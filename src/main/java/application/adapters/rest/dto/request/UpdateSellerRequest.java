package application.adapters.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateSellerRequest(
        @NotBlank String taxId) {
}
