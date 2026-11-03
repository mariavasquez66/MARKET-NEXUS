package application.adapters.persistence.jpa.entities;

import application.adapters.persistence.jpa.mappers.converters.CommercialStatusConverter;
import application.domain.enums.CommercialStatus;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "buyers")
public class BuyerJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "user_id", length = 36, nullable = false, unique = true)
    private String userId;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "main_street", length = 255)),
            @AttributeOverride(name = "city", column = @Column(name = "main_city", length = 255)),
            @AttributeOverride(name = "region", column = @Column(name = "main_region", length = 255)),
            @AttributeOverride(name = "postalCode", column = @Column(name = "main_postal_code", length = 20)),
            @AttributeOverride(name = "country", column = @Column(name = "main_country", length = 255))
    })
    private AddressEmbeddable mainAddress;

    @ElementCollection
    @CollectionTable(
            name = "buyer_additional_addresses",
            joinColumns = @JoinColumn(name = "buyer_id")
    )
    private List<AddressEmbeddable> additionalAddresses = new ArrayList<>();

    @Convert(converter = CommercialStatusConverter.class)
    @Column(name = "commercial_status", length = 40, nullable = false)
    private CommercialStatus commercialStatusCode;
}
