package application.domain.exceptions.product;

import application.domain.exceptions.common.DomainException;

public class InvalidPriceException extends DomainException {
    public InvalidPriceException(String message) {
        super(message);
    }

    public InvalidPriceException(String message, Throwable cause) {
        super(message, cause);
    }
}
