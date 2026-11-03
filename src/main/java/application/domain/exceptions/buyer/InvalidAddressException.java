package application.domain.exceptions.buyer;

import application.domain.exceptions.common.DomainException;

public class InvalidAddressException extends DomainException {
    public InvalidAddressException(String message) {
        super(message);
    }

    public InvalidAddressException(String message, Throwable cause) {
        super(message, cause);
    }
}
