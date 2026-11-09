package application.adapters.persistence.jpa.entities;

import application.adapters.persistence.jpa.mappers.converters.MovementTypeConverter;
import application.domain.enums.MovementType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inventory_movements", indexes = {
        @Index(name = "idx_inventory_movements_inventory", columnList = "inventory_id")
})
public class InventoryMovementJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "inventory_id", length = 36, nullable = false)
    private String inventoryId;

    @Convert(converter = MovementTypeConverter.class)
    @Column(name = "type", length = 40, nullable = false)
    private MovementType typeCode;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "performed_by_user_id", length = 36, nullable = false)
    private String performedByUserId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
