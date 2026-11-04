package application.adapters.rest.dto.response;

import java.time.Instant;
import java.time.LocalDate;

public record SalesReportResponse(
        LocalDate from,
        LocalDate to,
        int orderCount,
        MoneyResponse grossRevenue,
        String currency,
        Instant generatedAt) {
}
