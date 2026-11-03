package application.domain.exceptions.common;

public class SingleRoleException extends DomainException {
    public SingleRoleException(String message) {
        super(message);
    }

    public SingleRoleException(String message, Throwable cause) {
        super(message, cause);
    }
}
