package application.domain.exceptions.logistics;

import application.domain.exceptions.common.DomainException;

public class InvalidDestinationAddressException extends DomainException {
    public InvalidDestinationAddressException(String message) {
        super(message);
    }

    public InvalidDestinationAddressException(String message, Throwable cause) {
        super(message, cause);
    }
}
