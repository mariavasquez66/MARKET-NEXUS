package application.adapters.persistence.jpa.entities;

import application.adapters.persistence.jpa.mappers.converters.InventoryConditionConverter;
import application.domain.enums.InventoryCondition;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "inventories",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "warehouse_id"}),
        indexes = {
                @Index(name = "idx_inventories_product", columnList = "product_id"),
                @Index(name = "idx_inventories_warehouse", columnList = "warehouse_id")
        }
)
public class InventoryJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "product_id", length = 36, nullable = false)
    private String productId;

    @Column(name = "warehouse_id", length = 36, nullable = false)
    private String warehouseId;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Convert(converter = InventoryConditionConverter.class)
    @Column(name = "condition_code", length = 40, nullable = false)
    private InventoryCondition conditionCode;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
