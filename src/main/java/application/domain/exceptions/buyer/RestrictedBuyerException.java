package application.domain.exceptions.buyer;

import application.domain.exceptions.common.DomainException;

public class RestrictedBuyerException extends DomainException {
    public RestrictedBuyerException(String message) {
        super(message);
    }

    public RestrictedBuyerException(String message, Throwable cause) {
        super(message, cause);
    }
}
