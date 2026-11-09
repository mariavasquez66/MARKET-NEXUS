package application.adapters.useCases;

import application.domain.enums.ProductType;
import application.domain.enums.VariantType;
import application.domain.models.Inventory;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.User;
import application.domain.ports.in.BusinessCustomerPort;
import application.domain.services.CatalogService;
import application.domain.services.InventoryService;
import application.domain.services.OrderService;
import application.domain.valueobjects.Money;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class BusinessCustomerUseCase implements BusinessCustomerPort {

    private final CatalogService catalogService;
    private final InventoryService inventoryService;
    private final OrderService orderService;

    public BusinessCustomerUseCase(CatalogService catalogService,
                                   InventoryService inventoryService,
                                   OrderService orderService) {
        this.catalogService = catalogService;
        this.inventoryService = inventoryService;
        this.orderService = orderService;
    }

    @Override
    public Product registerProduct(User actor, String name, String description,
                                   ProductType type, Money price) {
        return catalogService.registerProduct(actor, name, description, type, price);
    }

    @Override
    public List<Product> listProducts(User actor) {
        return catalogService.listMyProducts(actor);
    }

    @Override
    public Product getProduct(User actor, String productId) {
        return catalogService.getProductForSeller(actor, productId);
    }

    @Override
    public Product updateProduct(User actor, String productId, String name,
                                 String description, Money price) {
        return catalogService.updateProduct(actor, productId, name, description, price);
    }

    @Override
    public Product addVariant(User actor, String productId, VariantType type, String value) {
        return catalogService.addVariant(actor, productId, type, value);
    }

    @Override
    public Product publishProduct(User actor, String productId) {
        return catalogService.publishProduct(actor, productId);
    }

    @Override
    public Product suspendProduct(User actor, String productId) {
        return catalogService.suspendProduct(actor, productId);
    }

    @Override
    public Product discontinueProduct(User actor, String productId) {
        return catalogService.discontinueProduct(actor, productId);
    }

    @Override
    public Inventory registerInventory(User actor, String productId,
                                       String warehouseId, int initialQuantity) {
        return inventoryService.registerInitialStock(actor, productId, warehouseId, initialQuantity);
    }

    @Override
    public List<Inventory> listInventory(User actor) {
        return inventoryService.listMyInventory(actor);
    }

    @Override
    public List<Order> listOrders(User actor) {
        return orderService.listOrders(actor);
    }
}
