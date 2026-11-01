package application.domain.exceptions.cart;

import application.domain.exceptions.common.DomainException;

public class InvalidQuantityException extends DomainException {
    public InvalidQuantityException(String message) {
        super(message);
    }

    public InvalidQuantityException(String message, Throwable cause) {
        super(message, cause);
    }
}
