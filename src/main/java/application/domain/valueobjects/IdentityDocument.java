package application.domain.valueobjects;

import application.domain.exceptions.common.InvalidDataException;
import java.util.Objects;
import java.util.regex.Pattern;

public final class IdentityDocument {
    private static final Pattern FORMAT = Pattern.compile("^[A-Z0-9-]{5,30}$");

    private final String value;

    private IdentityDocument(String value) {
        this.value = value;
    }

    public static IdentityDocument of(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidDataException("Invalid identity document");
        }
        String normalized = value.trim().toUpperCase();
        if (!FORMAT.matcher(normalized).matches()) {
            throw new InvalidDataException("Invalid identity document");
        }
        return new IdentityDocument(normalized);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IdentityDocument that)) {
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
        return value;
    }
}
