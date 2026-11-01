package application.domain.exceptions.inventory;

import application.domain.exceptions.common.DomainException;

public class InvalidReservationException extends DomainException {
    public InvalidReservationException(String message) {
        super(message);
    }

    public InvalidReservationException(String message, Throwable cause) {
        super(message, cause);
    }
}
