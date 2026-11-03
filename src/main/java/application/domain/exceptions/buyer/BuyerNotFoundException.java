package application.domain.exceptions.buyer;

import application.domain.exceptions.common.DomainException;

public class BuyerNotFoundException extends DomainException {
    public BuyerNotFoundException(String message) {
        super(message);
    }

    public BuyerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
