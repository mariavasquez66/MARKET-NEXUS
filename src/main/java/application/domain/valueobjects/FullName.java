package application.domain.valueobjects;

import application.domain.exceptions.common.InvalidDataException;
import java.util.Objects;

public final class FullName {
    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 120;

    private final String value;

    private FullName(String value) {
        this.value = value;
    }

    public static FullName of(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidDataException("Full name is required");
        }
        String normalized = value.trim();
        if (normalized.length() < MIN_LENGTH || normalized.length() > MAX_LENGTH) {
            throw new InvalidDataException("Full name length must be between 2 and 120");
        }
        return new FullName(normalized);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FullName fullName)) {
            return false;
        }
        return value.equals(fullName.value);
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
