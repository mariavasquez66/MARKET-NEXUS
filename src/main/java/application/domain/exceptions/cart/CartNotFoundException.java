package application.domain.exceptions.cart;

import application.domain.exceptions.common.DomainException;

public class CartNotFoundException extends DomainException {
    public CartNotFoundException(String message) {
        super(message);
    }

    public CartNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
