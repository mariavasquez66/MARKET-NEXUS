package application.adapters.persistence.jpa.entities;

import application.adapters.persistence.jpa.mappers.converters.RefundStatusConverter;
import application.domain.enums.RefundStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "refunds")
public class RefundJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "return_id", length = 36, nullable = false)
    private String returnId;

    @Column(name = "amount_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amountAmount;

    @Column(name = "amount_currency", length = 3, nullable = false)
    private String amountCurrency;

    @Convert(converter = RefundStatusConverter.class)
    @Column(name = "status", length = 40, nullable = false)
    private RefundStatus statusCode;
}
