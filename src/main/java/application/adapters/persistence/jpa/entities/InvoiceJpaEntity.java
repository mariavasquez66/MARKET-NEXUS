package application.adapters.persistence.jpa.entities;

import application.adapters.persistence.jpa.mappers.converters.InvoiceStatusConverter;
import application.domain.enums.InvoiceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "invoices")
public class InvoiceJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "order_id", length = 36, nullable = false, unique = true)
    private String orderId;

    @Column(name = "number", length = 100, nullable = false, unique = true)
    private String number;

    @Column(name = "subtotal_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal subtotalAmount;

    @Column(name = "subtotal_currency", length = 3, nullable = false)
    private String subtotalCurrency;

    @Column(name = "taxes_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal taxesAmount;

    @Column(name = "taxes_currency", length = 3, nullable = false)
    private String taxesCurrency;

    @Column(name = "total_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "total_currency", length = 3, nullable = false)
    private String totalCurrency;

    @Convert(converter = InvoiceStatusConverter.class)
    @Column(name = "status", length = 40, nullable = false)
    private InvoiceStatus statusCode;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;
}
