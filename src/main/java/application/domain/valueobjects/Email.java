package application.domain.valueobjects;

import application.domain.exceptions.common.InvalidDataException;
import java.util.Objects;
import java.util.regex.Pattern;

public final class Email {
    private static final Pattern FORMAT =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final String value;

    private Email(String value) {
        this.value = value;
    }

    public static Email of(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidDataException("Email is required");
        }
        String normalized = value.trim().toLowerCase();
        if (!FORMAT.matcher(normalized).matches()) {
            throw new InvalidDataException("Invalid email: " + value);
        }
        return new Email(normalized);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Email email)) {
            return false;
        }
        return value.equals(email.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
