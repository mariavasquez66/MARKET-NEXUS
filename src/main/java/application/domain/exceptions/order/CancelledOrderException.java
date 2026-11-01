package application.domain.exceptions.order;

import application.domain.exceptions.common.DomainException;

public class CancelledOrderException extends DomainException {
    public CancelledOrderException(String message) {
        super(message);
    }

    public CancelledOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}
