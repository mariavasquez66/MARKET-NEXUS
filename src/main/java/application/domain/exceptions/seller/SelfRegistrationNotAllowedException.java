package application.domain.exceptions.seller;

import application.domain.exceptions.common.DomainException;

public class SelfRegistrationNotAllowedException extends DomainException {
    public SelfRegistrationNotAllowedException(String message) {
        super(message);
    }

    public SelfRegistrationNotAllowedException(String message, Throwable cause) {
        super(message, cause);
    }
}
