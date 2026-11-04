package application.adapters.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddVariantRequest(
        @NotBlank String type,
        @NotBlank @Size(max = 50) String value) {
}
