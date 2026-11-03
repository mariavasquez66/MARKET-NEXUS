package application.adapters.persistence.jpa.mappers.converters;

import application.domain.enums.ProductStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ProductStatusConverter implements AttributeConverter<ProductStatus, String> {

    @Override
    public String convertToDatabaseColumn(ProductStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public ProductStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ProductStatus.fromCode(dbData);
    }
}
