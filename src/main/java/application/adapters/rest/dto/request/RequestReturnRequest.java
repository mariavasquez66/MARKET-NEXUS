package application.adapters.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RequestReturnRequest(
        @NotBlank @Size(max = 1000) String reason,
        @Size(max = 36) String productId,
        @Positive Integer quantity) {
}
