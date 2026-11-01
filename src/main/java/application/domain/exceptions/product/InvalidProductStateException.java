package application.domain.exceptions.product;

import application.domain.exceptions.common.DomainException;

public class InvalidProductStateException extends DomainException {
    public InvalidProductStateException(String message) {
        super(message);
    }

    public InvalidProductStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
