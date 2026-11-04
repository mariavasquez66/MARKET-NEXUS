package application.adapters.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StartShipmentRequest(
        @NotBlank @Size(max = 100) String trackingNumber) {
}
