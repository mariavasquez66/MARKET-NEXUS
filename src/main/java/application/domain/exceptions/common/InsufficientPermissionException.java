package application.domain.exceptions.common;

public class InsufficientPermissionException extends DomainException {
    public InsufficientPermissionException(String message) {
        super(message);
    }

    public InsufficientPermissionException(String message, Throwable cause) {
        super(message, cause);
    }
}
