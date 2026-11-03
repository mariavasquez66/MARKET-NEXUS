package application.adapters.rest.dto.response;

import java.math.BigDecimal;

public record MoneyResponse(BigDecimal amount, String currency) {
}
