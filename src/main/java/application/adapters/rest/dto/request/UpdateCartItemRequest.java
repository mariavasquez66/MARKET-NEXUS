package application.adapters.rest.dto.request;

import jakarta.validation.constraints.Positive;

public record UpdateCartItemRequest(
        @Positive int quantity) {
}
