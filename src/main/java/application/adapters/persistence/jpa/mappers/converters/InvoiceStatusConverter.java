package application.adapters.persistence.jpa.mappers.converters;

import application.domain.enums.InvoiceStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class InvoiceStatusConverter implements AttributeConverter<InvoiceStatus, String> {

    @Override
    public String convertToDatabaseColumn(InvoiceStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public InvoiceStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : InvoiceStatus.fromCode(dbData);
    }
}
