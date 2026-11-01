package application.domain.exceptions.user;

import application.domain.exceptions.common.DomainException;

public class InactiveUserException extends DomainException {
    public InactiveUserException(String message) {
        super(message);
    }

    public InactiveUserException(String message, Throwable cause) {
        super(message, cause);
    }
}
