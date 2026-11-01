package application.domain.enums;

import application.domain.exceptions.common.InvalidDataException;
import java.util.HashMap;
import java.util.Map;

public class ProductStatus extends DomainCatalog {
    public static final ProductStatus PUBLISHED = new ProductStatus("PUBLISHED", "Published");
    public static final ProductStatus SUSPENDED = new ProductStatus("SUSPENDED", "Suspended");
    public static final ProductStatus DISCONTINUED = new ProductStatus("DISCONTINUED", "Discontinued");

    private static final Map<String, ProductStatus> VALUES = new HashMap<>();

    static {
        VALUES.put("PUBLISHED", PUBLISHED);
        VALUES.put("SUSPENDED", SUSPENDED);
        VALUES.put("DISCONTINUED", DISCONTINUED);
    }

    private ProductStatus(String code, String description) {
        super(code, description);
    }

    public static ProductStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidDataException("ProductStatus code cannot be null or blank");
        }
        ProductStatus status = VALUES.get(code.toUpperCase());
        if (status == null) {
            throw new InvalidDataException("Invalid ProductStatus code: " + code);
        }
        return status;
    }
}
