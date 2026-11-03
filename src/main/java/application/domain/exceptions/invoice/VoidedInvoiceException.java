package application.domain.exceptions.invoice;

import application.domain.exceptions.common.DomainException;

public class VoidedInvoiceException extends DomainException {
    public VoidedInvoiceException(String message) {
        super(message);
    }

    public VoidedInvoiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
