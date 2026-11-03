package application.domain.valueobjects;

import application.domain.exceptions.common.InvalidDataException;
import java.util.Objects;

public final class PasswordHash {
    private final String value;

    private PasswordHash(String value) {
        this.value = value;
    }

    public static PasswordHash of(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidDataException("Password hash is required");
        }
        return new PasswordHash(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PasswordHash that)) {
            return false;
        }
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "***";
    }
}
