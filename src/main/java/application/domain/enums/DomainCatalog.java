package application.domain.enums;

import java.util.Objects;

public abstract class DomainCatalog {
    private final String code;
    private final String description;

    protected DomainCatalog(String code, String description) {
        this.code = Objects.requireNonNull(code, "code cannot be null");
        this.description = Objects.requireNonNull(description, "description cannot be null");
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainCatalog)) return false;
        DomainCatalog that = (DomainCatalog) o;
        return Objects.equals(code, that.code) && getClass().equals(that.getClass());
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, getClass());
    }

    @Override
    public String toString() {
        return code;
    }
}
