package application.domain.exceptions.common;

public class UnauthenticatedUserException extends DomainException {
    public UnauthenticatedUserException(String message) {
        super(message);
    }

    public UnauthenticatedUserException(String message, Throwable cause) {
        super(message, cause);
    }
}
