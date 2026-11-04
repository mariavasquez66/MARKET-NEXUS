package application.adapters.rest.mappers;

import application.adapters.rest.dto.response.InvoiceResponse;
import application.domain.models.Invoice;
import org.springframework.stereotype.Component;

@Component
public class InvoiceRestMapper extends RestMapperSupport {

    public InvoiceResponse toResponse(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getOrder().getId(),
                invoice.getNumber(),
                toMoneyResponse(invoice.getSubtotal()),
                toMoneyResponse(invoice.getTaxes()),
                toMoneyResponse(invoice.getTotal()),
                invoice.getStatus().getCode(),
                invoice.getIssuedAt());
    }
}
