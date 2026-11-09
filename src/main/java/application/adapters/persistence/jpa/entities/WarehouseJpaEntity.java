package application.adapters.persistence.jpa.entities;

import application.adapters.persistence.jpa.mappers.converters.WarehouseTypeConverter;
import application.domain.enums.WarehouseType;
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
@Table(name = "warehouses", indexes = {
        @Index(name = "idx_warehouses_owner", columnList = "owner_seller_id"),
        @Index(name = "idx_warehouses_responsible", columnList = "responsible_user_id")
})
public class WarehouseJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "location_street", length = 255)),
            @AttributeOverride(name = "city", column = @Column(name = "location_city", length = 255)),
            @AttributeOverride(name = "region", column = @Column(name = "location_region", length = 255)),
            @AttributeOverride(name = "postalCode", column = @Column(name = "location_postal_code", length = 20)),
            @AttributeOverride(name = "country", column = @Column(name = "location_country", length = 255))
    })
    private AddressEmbeddable location;

    @Convert(converter = WarehouseTypeConverter.class)
    @Column(name = "type", length = 40, nullable = false)
    private WarehouseType typeCode;

    @Column(name = "owner_seller_id", length = 36)
    private String ownerSellerId;

    @Column(name = "responsible_user_id", length = 36, nullable = false)
    private String responsibleUserId;
}
