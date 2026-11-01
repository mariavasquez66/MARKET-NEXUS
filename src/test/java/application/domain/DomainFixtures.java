package application.domain;

import application.domain.enums.ProductType;
import application.domain.enums.UserRole;
import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.valueobjects.Address;
import application.domain.valueobjects.Email;
import application.domain.valueobjects.FullName;
import application.domain.valueobjects.IdentityDocument;
import application.domain.valueobjects.Money;
import application.domain.valueobjects.PasswordHash;
import application.domain.valueobjects.TaxId;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

public final class DomainFixtures {
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private DomainFixtures() {
    }

    public static Address address() {
        return Address.of("Calle 1 # 2-3", "Bogota", "Cundinamarca", "110111", "Colombia");
    }

    public static User user(UserRole role) {
        int value = SEQUENCE.incrementAndGet();
        return User.create(
                FullName.of("User " + value),
                Email.of("user" + value + "@nexusmarket.test"),
                IdentityDocument.of("DOC" + (100000 + value)),
                role,
                PasswordHash.of("$2a$10$hash" + value));
    }

    public static User admin() {
        return user(UserRole.ADMIN);
    }

    public static User operator() {
        return user(UserRole.LOGISTIC_OPERATOR);
    }

    public static Buyer buyer() {
        return Buyer.create(user(UserRole.BUYER), address());
    }

    public static Seller seller(User admin) {
        return Seller.registerBy(admin, user(UserRole.SELLER), TaxId.of("900123456-7"));
    }

    public static Warehouse sellerWarehouse(Seller seller, User responsible) {
        Warehouse warehouse = Warehouse.createSellerWarehouse(
                "Seller warehouse", address(), seller, responsible);
        seller.assignWarehouse(warehouse);
        return warehouse;
    }

    public static Product publishedPhysicalProduct(Seller seller, User responsible) {
        sellerWarehouse(seller, responsible);
        Product product = Product.create(seller, "Laptop", "Laptop description",
                ProductType.PHYSICAL, Money.cop(new BigDecimal("119.00")));
        product.publish();
        return product;
    }

    public static Product publishedDigitalProduct(Seller seller) {
        Product product = Product.create(seller, "Course", "Digital course",
                ProductType.DIGITAL, Money.cop(new BigDecimal("119.00")));
        product.publish();
        return product;
    }

    public static Cart cartWith(Buyer buyer, Product product, int quantity) {
        Cart cart = Cart.create(buyer);
        cart.addItem(product, quantity);
        return cart;
    }
}
