package application.domain.ports.in;

import application.domain.enums.ProductType;
import application.domain.enums.VariantType;
import application.domain.models.Inventory;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.User;
import application.domain.valueobjects.Money;
import java.util.List;

public interface BusinessCustomerPort {

    Product registerProduct(User actor, String name, String description,
                            ProductType type, Money price);

    List<Product> listProducts(User actor);

    Product getProduct(User actor, String productId);

    Product updateProduct(User actor, String productId, String name,
                          String description, Money price);

    Product addVariant(User actor, String productId, VariantType type, String value);

    Product publishProduct(User actor, String productId);

    Product suspendProduct(User actor, String productId);

    Product discontinueProduct(User actor, String productId);

    Inventory registerInventory(User actor, String productId,
                                String warehouseId, int initialQuantity);

    List<Inventory> listInventory(User actor);

    List<Order> listOrders(User actor);
}
