package application.adapters.persistence.jpa.entities;

import application.adapters.persistence.jpa.mappers.converters.ReturnStatusConverter;
import application.domain.enums.ReturnStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "returns")
public class ReturnJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "order_id", length = 36, nullable = false)
    private String orderId;

    @Column(name = "reason", length = 1000, nullable = false)
    private String reason;

    @Convert(converter = ReturnStatusConverter.class)
    @Column(name = "status", length = 40, nullable = false)
    private ReturnStatus statusCode;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;
}
