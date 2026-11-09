package application.adapters.persistence.jpa.entities;

import application.adapters.persistence.jpa.mappers.converters.ShipmentStatusConverter;
import application.domain.enums.ShipmentStatus;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
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
@Table(name = "shipments", indexes = {
        @Index(name = "idx_shipments_order", columnList = "order_id"),
        @Index(name = "idx_shipments_origin", columnList = "origin_warehouse_id"),
        @Index(name = "idx_shipments_order_origin", columnList = "order_id,origin_warehouse_id")
})
public class ShipmentJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "order_id", length = 36, nullable = false)
    private String orderId;

    @Column(name = "origin_warehouse_id", length = 36, nullable = false)
    private String originWarehouseId;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "destination_street", length = 255)),
            @AttributeOverride(name = "city", column = @Column(name = "destination_city", length = 255)),
            @AttributeOverride(name = "region", column = @Column(name = "destination_region", length = 255)),
            @AttributeOverride(name = "postalCode", column = @Column(name = "destination_postal_code", length = 20)),
            @AttributeOverride(name = "country", column = @Column(name = "destination_country", length = 255))
    })
    private AddressEmbeddable destination;

    @Column(name = "tracking_number", length = 255)
    private String trackingNumber;

    @Convert(converter = ShipmentStatusConverter.class)
    @Column(name = "status", length = 40, nullable = false)
    private ShipmentStatus statusCode;
}
