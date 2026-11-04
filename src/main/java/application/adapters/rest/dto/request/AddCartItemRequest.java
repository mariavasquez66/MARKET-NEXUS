package application.adapters.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record AddCartItemRequest(
        @NotBlank String productId,
        @Positive int quantity) {
}
