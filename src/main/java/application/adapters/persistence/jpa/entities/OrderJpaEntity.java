package application.adapters.persistence.jpa.entities;

import application.adapters.persistence.jpa.mappers.converters.OrderStatusConverter;
import application.adapters.persistence.jpa.mappers.converters.PaymentStatusConverter;
import application.domain.enums.OrderStatus;
import application.domain.enums.PaymentStatus;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_buyer", columnList = "buyer_id"),
        @Index(name = "idx_orders_status", columnList = "status"),
        @Index(name = "idx_orders_payment_created", columnList = "payment_status,created_at")
})
public class OrderJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "buyer_id", length = 36, nullable = false)
    private String buyerId;

    @Convert(converter = OrderStatusConverter.class)
    @Column(name = "status", length = 40, nullable = false)
    private OrderStatus statusCode;

    @Column(name = "total_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "total_currency", length = 3, nullable = false)
    private String totalCurrency;

    @Convert(converter = PaymentStatusConverter.class)
    @Column(name = "payment_status", length = 40, nullable = false)
    private PaymentStatus paymentStatusCode;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "delivery_street", length = 255)),
            @AttributeOverride(name = "city", column = @Column(name = "delivery_city", length = 255)),
            @AttributeOverride(name = "region", column = @Column(name = "delivery_region", length = 255)),
            @AttributeOverride(name = "postalCode", column = @Column(name = "delivery_postal_code", length = 20)),
            @AttributeOverride(name = "country", column = @Column(name = "delivery_country", length = 255))
    })
    private AddressEmbeddable deliveryAddress;

    @Column(name = "invoice_id", length = 36)
    private String invoiceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
