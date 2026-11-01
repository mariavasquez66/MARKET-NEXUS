package application.domain.exceptions.inventory;

import application.domain.exceptions.common.DomainException;

public class InsufficientStockException extends DomainException {
    public InsufficientStockException(String message) {
        super(message);
    }

    public InsufficientStockException(String message, Throwable cause) {
        super(message, cause);
    }
}
