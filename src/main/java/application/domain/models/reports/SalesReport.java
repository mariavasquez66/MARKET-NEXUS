package application.domain.models.reports;

import application.domain.exceptions.common.InvalidDataException;
import application.domain.valueobjects.Money;
import java.time.LocalDate;
import java.util.Objects;

public record SalesReport(LocalDate from, LocalDate to, int orderCount, Money totalSales) {
    public SalesReport {
        Objects.requireNonNull(from, "from cannot be null");
        Objects.requireNonNull(to, "to cannot be null");
        if (from.isAfter(to)) {
            throw new InvalidDataException("Date range is invalid");
        }
        if (orderCount < 0) {
            throw new InvalidDataException("Order count cannot be negative");
        }
        Objects.requireNonNull(totalSales, "totalSales cannot be null");
    }
}
