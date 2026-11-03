package application.domain.valueobjects;

import application.domain.exceptions.common.InvalidDataException;
import java.util.Objects;
import java.util.regex.Pattern;

public final class TaxId {
    private static final Pattern FORMAT = Pattern.compile("^[0-9]{9,15}(-[0-9])?$");

    private final String value;

    private TaxId(String value) {
        this.value = value;
    }

    public static TaxId of(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidDataException("Invalid tax id");
        }
        String normalized = value.trim();
        if (!FORMAT.matcher(normalized).matches()) {
            throw new InvalidDataException("Invalid tax id");
        }
        return new TaxId(normalized);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TaxId taxId)) {
            return false;
        }
        return value.equals(taxId.value);
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
