package application.adapters.rest.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record GenerateRefundRequest(
        @NotNull @DecimalMin("0.01") BigDecimal amount) {
}
