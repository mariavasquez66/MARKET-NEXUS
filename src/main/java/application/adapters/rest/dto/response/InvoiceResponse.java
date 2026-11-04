package application.adapters.rest.dto.response;

import java.time.Instant;

public record InvoiceResponse(
        String id,
        String orderId,
        String number,
        MoneyResponse subtotal,
        MoneyResponse taxes,
        MoneyResponse total,
        String status,
        Instant issuedAt) {
}
