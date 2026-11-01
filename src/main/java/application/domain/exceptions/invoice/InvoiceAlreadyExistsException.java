package application.domain.exceptions.invoice;

import application.domain.exceptions.common.DomainException;

public class InvoiceAlreadyExistsException extends DomainException {
    public InvoiceAlreadyExistsException(String message) {
        super(message);
    }

    public InvoiceAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
