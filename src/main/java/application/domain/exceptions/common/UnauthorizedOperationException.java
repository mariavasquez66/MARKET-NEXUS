package application.domain.exceptions.common;

public class UnauthorizedOperationException extends DomainException {
    public UnauthorizedOperationException(String message) {
        super(message);
    }

    public UnauthorizedOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
