package application.domain.models;

import application.domain.enums.ProductStatus;
import application.domain.enums.ProductType;
import application.domain.exceptions.common.InvalidDataException;
import application.domain.exceptions.product.InvalidPriceException;
import application.domain.exceptions.product.InvalidProductStateException;
import application.domain.exceptions.seller.SellerWithoutWarehouseException;
import application.domain.valueobjects.Money;
import application.domain.valueobjects.ProductVariant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Product {
    private final String id;
    private final Seller seller;
    private String name;
    private String description;
    private final ProductType type;
    private ProductStatus status;
    private Money price;
    private final List<ProductVariant> variants;

    private Product(String id,
                    Seller seller,
                    String name,
                    String description,
                    ProductType type,
                    ProductStatus status,
                    Money price,
                    List<ProductVariant> variants) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.seller = Objects.requireNonNull(seller, "seller cannot be null");
        this.name = requireText(name, "Product name is required");
        this.description = Objects.requireNonNull(description, "description cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.price = requirePositive(price);
        this.variants = new ArrayList<>(Objects.requireNonNull(variants));
    }

    public static Product create(Seller seller,
                                 String name,
                                 String description,
                                 ProductType type,
                                 Money price) {
        return new Product(UUID.randomUUID().toString(), seller, name, description,
                type, ProductStatus.SUSPENDED, price, List.of());
    }

    public static Product reconstitute(String id,
                                       Seller seller,
                                       String name,
                                       String description,
                                       ProductType type,
                                       ProductStatus status,
                                       Money price,
                                       List<ProductVariant> variants) {
        return new Product(id, seller, name, description, type, status, price, variants);
    }

    public void publish() {
        if (status.equals(ProductStatus.DISCONTINUED)) {
            throw new InvalidProductStateException("Discontinued product cannot be published");
        }
        if (requiresInventory() && !seller.hasWarehouses()) {
            throw new SellerWithoutWarehouseException("Physical product requires seller warehouse");
        }
        status = ProductStatus.PUBLISHED;
    }

    public void suspend() {
        if (status.equals(ProductStatus.DISCONTINUED)) {
            throw new InvalidProductStateException("Discontinued product cannot be suspended");
        }
        if (!status.equals(ProductStatus.PUBLISHED)) {
            throw new InvalidProductStateException("Only published products can be suspended");
        }
        status = ProductStatus.SUSPENDED;
    }

    public void discontinue() {
        if (status.equals(ProductStatus.DISCONTINUED)) {
            throw new InvalidProductStateException("Product is already discontinued");
        }
        status = ProductStatus.DISCONTINUED;
    }

    public void changePrice(Money price) {
        this.price = requirePositive(price);
    }

    public void changeDetails(String name, String description) {
        if (status.equals(ProductStatus.DISCONTINUED)) {
            throw new InvalidProductStateException("Discontinued product cannot be modified");
        }
        this.name = requireText(name, "Product name is required");
        this.description = Objects.requireNonNull(description, "description cannot be null");
    }

    public void addVariant(ProductVariant variant) {
        Objects.requireNonNull(variant, "variant cannot be null");
        if (variants.contains(variant)) {
            throw new InvalidDataException("Variant already exists");
        }
        variants.add(variant);
    }

    public void removeVariant(ProductVariant variant) {
        if (!variants.remove(variant)) {
            throw new InvalidDataException("Variant does not exist");
        }
    }

    public boolean requiresInventory() {
        return type.equals(ProductType.PHYSICAL);
    }

    public boolean requiresShipment() {
        return type.equals(ProductType.PHYSICAL);
    }

    public boolean isPurchasable() {
        return status.equals(ProductStatus.PUBLISHED);
    }

    private static Money requirePositive(Money price) {
        if (price == null || !price.isPositive()) {
            throw new InvalidPriceException("Product price must be greater than zero");
        }
        return price;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidDataException(message);
        }
        return value.trim();
    }

    public String getId() {
        return id;
    }

    public Seller getSeller() {
        return seller;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ProductType getType() {
        return type;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public Money getPrice() {
        return price;
    }

    public List<ProductVariant> getVariants() {
        return Collections.unmodifiableList(variants);
    }
}
