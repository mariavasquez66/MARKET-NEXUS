package application.adapters.persistence.jpa.entities;

import application.adapters.persistence.jpa.mappers.converters.ProductStatusConverter;
import application.adapters.persistence.jpa.mappers.converters.ProductTypeConverter;
import application.domain.enums.ProductStatus;
import application.domain.enums.ProductType;
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
@Table(name = "products")
public class ProductJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "seller_id", length = 36, nullable = false)
    private String sellerId;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Convert(converter = ProductTypeConverter.class)
    @Column(name = "type", length = 40, nullable = false)
    private ProductType typeCode;

    @Convert(converter = ProductStatusConverter.class)
    @Column(name = "status", length = 40, nullable = false)
    private ProductStatus statusCode;

    @Column(name = "price_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal priceAmount;

    @Column(name = "price_currency", length = 3, nullable = false)
    private String priceCurrency;
}
