package application.domain.exceptions.cart;

import application.domain.exceptions.common.DomainException;

public class ProductNotAvailableException extends DomainException {
    public ProductNotAvailableException(String message) {
        super(message);
    }

    public ProductNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
