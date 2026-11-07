package application.adapters.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "order_allocations", indexes = {
        @Index(name = "idx_order_allocations_order", columnList = "order_id"),
        @Index(name = "idx_order_allocations_warehouse", columnList = "warehouse_id")
})
public class OrderAllocationJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "order_id", length = 36, nullable = false)
    private String orderId;

    @Column(name = "product_id", length = 36, nullable = false)
    private String productId;

    @Column(name = "warehouse_id", length = 36, nullable = false)
    private String warehouseId;

    @Column(name = "quantity", nullable = false)
    private int quantity;
}
