package application.domain.exceptions.user;

import application.domain.exceptions.common.DomainException;

public class InvalidRoleException extends DomainException {
    public InvalidRoleException(String message) {
        super(message);
    }

    public InvalidRoleException(String message, Throwable cause) {
        super(message, cause);
    }
}
