package application.domain.exceptions.cart;

import application.domain.exceptions.common.DomainException;

public class EmptyCartException extends DomainException {
    public EmptyCartException(String message) {
        super(message);
    }

    public EmptyCartException(String message, Throwable cause) {
        super(message, cause);
    }
}
