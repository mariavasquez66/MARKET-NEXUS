package application.domain.models;

import application.domain.enums.InvoiceStatus;
import application.domain.enums.OrderStatus;
import application.domain.exceptions.common.InvalidDataException;
import application.domain.exceptions.invoice.VoidedInvoiceException;
import application.domain.exceptions.order.InvalidStateTransitionException;
import application.domain.valueobjects.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Invoice {
    private final String id;
    private final Order order;
    private final String number;
    private final Money subtotal;
    private final Money taxes;
    private final Money total;
    private InvoiceStatus status;
    private final Instant issuedAt;

    private Invoice(String id,
                    Order order,
                    String number,
                    Money subtotal,
                    Money taxes,
                    Money total,
                    InvoiceStatus status,
                    Instant issuedAt) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.order = Objects.requireNonNull(order, "order cannot be null");
        if (number == null || number.isBlank()) {
            throw new InvalidDataException("Invoice number is required");
        }
        this.number = number.trim();
        this.subtotal = Objects.requireNonNull(subtotal, "subtotal cannot be null");
        this.taxes = Objects.requireNonNull(taxes, "taxes cannot be null");
        this.total = Objects.requireNonNull(total, "total cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt cannot be null");
    }

    public static Invoice create(Order order, String number, BigDecimal taxRate, Instant issuedAt) {
        Objects.requireNonNull(order, "order cannot be null");
        if (!order.getStatus().equals(OrderStatus.PAID)) {
            throw new InvalidStateTransitionException("Invoice requires a paid order");
        }
        if (taxRate == null || taxRate.signum() < 0) {
            throw new InvalidDataException("Tax rate cannot be negative");
        }
        Money total = order.getTotalAmount();
        BigDecimal subtotalAmount = total.getAmount()
                .divide(BigDecimal.ONE.add(taxRate), 2, RoundingMode.HALF_UP);
        Money subtotal = Money.of(subtotalAmount, total.getCurrency());
        Money taxes = total.subtract(subtotal);
        return new Invoice(UUID.randomUUID().toString(), order, number, subtotal, taxes,
                total, InvoiceStatus.ISSUED, issuedAt);
    }

    public static Invoice reconstitute(String id,
                                       Order order,
                                       String number,
                                       Money subtotal,
                                       Money taxes,
                                       Money total,
                                       InvoiceStatus status,
                                       Instant issuedAt) {
        return new Invoice(id, order, number, subtotal, taxes, total, status, issuedAt);
    }

    public void voidInvoice() {
        if (status.equals(InvoiceStatus.VOIDED)) {
            throw new VoidedInvoiceException("Invoice is already voided");
        }
        status = InvoiceStatus.VOIDED;
    }

    public String getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public String getNumber() {
        return number;
    }

    public Money getSubtotal() {
        return subtotal;
    }

    public Money getTaxes() {
        return taxes;
    }

    public Money getTotal() {
        return total;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }
}
