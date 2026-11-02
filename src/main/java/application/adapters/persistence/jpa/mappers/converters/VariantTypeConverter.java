package application.adapters.persistence.jpa.mappers.converters;

import application.domain.enums.VariantType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class VariantTypeConverter implements AttributeConverter<VariantType, String> {

    @Override
    public String convertToDatabaseColumn(VariantType attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public VariantType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : VariantType.fromCode(dbData);
    }
}
