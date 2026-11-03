package application.domain.exceptions.product;

import application.domain.exceptions.common.DomainException;

public class InvalidProductTypeException extends DomainException {
    public InvalidProductTypeException(String message) {
        super(message);
    }

    public InvalidProductTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
