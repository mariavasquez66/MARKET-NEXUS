package application.domain.exceptions.logistics;

import application.domain.exceptions.common.DomainException;

public class ShipmentAlreadyStartedException extends DomainException {
    public ShipmentAlreadyStartedException(String message) {
        super(message);
    }

    public ShipmentAlreadyStartedException(String message, Throwable cause) {
        super(message, cause);
    }
}
