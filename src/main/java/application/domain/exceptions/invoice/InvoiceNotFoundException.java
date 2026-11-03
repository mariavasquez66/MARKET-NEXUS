package application.domain.exceptions.invoice;

import application.domain.exceptions.common.DomainException;

public class InvoiceNotFoundException extends DomainException {
    public InvoiceNotFoundException(String message) {
        super(message);
    }

    public InvoiceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
