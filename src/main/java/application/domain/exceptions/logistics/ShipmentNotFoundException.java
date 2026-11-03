package application.domain.exceptions.logistics;

import application.domain.exceptions.common.DomainException;

public class ShipmentNotFoundException extends DomainException {
    public ShipmentNotFoundException(String message) {
        super(message);
    }

    public ShipmentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
