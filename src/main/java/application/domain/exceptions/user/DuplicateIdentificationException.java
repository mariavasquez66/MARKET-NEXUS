package application.domain.exceptions.user;

import application.domain.exceptions.common.DomainException;

public class DuplicateIdentificationException extends DomainException {
    public DuplicateIdentificationException(String message) {
        super(message);
    }

    public DuplicateIdentificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
