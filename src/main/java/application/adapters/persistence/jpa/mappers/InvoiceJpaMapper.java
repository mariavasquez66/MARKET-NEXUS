package application.adapters.persistence.jpa.mappers;

import application.adapters.persistence.jpa.entities.InvoiceJpaEntity;
import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.valueobjects.Money;
import java.util.Currency;
import org.springframework.stereotype.Component;

@Component
public class InvoiceJpaMapper {

    public InvoiceJpaEntity toEntity(Invoice invoice) {
        InvoiceJpaEntity entity = new InvoiceJpaEntity();
        entity.setId(invoice.getId());
        entity.setOrderId(invoice.getOrder().getId());
        entity.setNumber(invoice.getNumber());
        entity.setSubtotalAmount(invoice.getSubtotal().getAmount());
        entity.setSubtotalCurrency(invoice.getSubtotal().getCurrency().getCurrencyCode());
        entity.setTaxesAmount(invoice.getTaxes().getAmount());
        entity.setTaxesCurrency(invoice.getTaxes().getCurrency().getCurrencyCode());
        entity.setTotalAmount(invoice.getTotal().getAmount());
        entity.setTotalCurrency(invoice.getTotal().getCurrency().getCurrencyCode());
        entity.setStatusCode(invoice.getStatus());
        entity.setIssuedAt(invoice.getIssuedAt());
        return entity;
    }

    public Invoice toDomain(InvoiceJpaEntity entity, Order order) {
        return Invoice.reconstitute(
                entity.getId(),
                order,
                entity.getNumber(),
                Money.of(entity.getSubtotalAmount(), Currency.getInstance(entity.getSubtotalCurrency())),
                Money.of(entity.getTaxesAmount(), Currency.getInstance(entity.getTaxesCurrency())),
                Money.of(entity.getTotalAmount(), Currency.getInstance(entity.getTotalCurrency())),
                entity.getStatusCode(),
                entity.getIssuedAt());
    }
}
