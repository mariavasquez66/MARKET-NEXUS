package application.domain.exceptions.common;

public class NoDataException extends DomainException {
    public NoDataException(String message) {
        super(message);
    }

    public NoDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
