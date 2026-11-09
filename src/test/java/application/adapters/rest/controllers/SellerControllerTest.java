package application.adapters.rest.controllers;

import application.adapters.rest.mappers.InventoryRestMapper;
import application.adapters.rest.mappers.OrderRestMapper;
import application.adapters.rest.mappers.ProductRestMapper;
import application.domain.DomainFixtures;
import application.domain.enums.UserRole;
import application.domain.enums.VariantType;
import application.domain.exceptions.product.InvalidProductStateException;
import application.domain.exceptions.product.ProductNotFoundException;
import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.Inventory;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.ports.in.BusinessCustomerPort;
import application.domain.valueobjects.ProductVariant;
import application.infrastructure.exception.GlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static application.adapters.rest.controllers.RestControllerTestSupport.actor;

class SellerControllerTest {

    private BusinessCustomerPort customerPort;
    private MockMvc mockMvc;
    private User actor;

    @BeforeEach
    void setUp() {
        customerPort = mock(BusinessCustomerPort.class);
        actor = DomainFixtures.user(UserRole.SELLER);
        SellerController controller = new SellerController(
                customerPort,
                new ProductRestMapper(),
                new InventoryRestMapper(),
                new OrderRestMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(RestControllerTestSupport.actorResolver())
                .build();
    }

    @Test
    void createProductReturns201() throws Exception {
        Product product = product();
        when(customerPort.registerProduct(any(), any(), any(), any(), any())).thenReturn(product);

        mockMvc.perform(post("/api/v1/seller/products")
                        .with(actor(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Laptop","description":"Laptop description","type":"PHYSICAL","price":119.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(product.getId()))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.type").value("PHYSICAL"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.price.amount").value(119.00))
                .andExpect(jsonPath("$.price.currency").value("COP"));
    }

    @Test
    void createProductRejectsInvalidPrice() throws Exception {
        mockMvc.perform(post("/api/v1/seller/products")
                        .with(actor(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Laptop","description":"Laptop description","type":"PHYSICAL","price":0.00}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createProductRejectsInvalidCatalogCode() throws Exception {
        mockMvc.perform(post("/api/v1/seller/products")
                        .with(actor(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Laptop","description":"Laptop description","type":"UNKNOWN","price":10.00}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATA"));
    }

    @Test
    void listProductsReturns200() throws Exception {
        Product product = product();
        when(customerPort.listProducts(actor)).thenReturn(List.of(product));

        mockMvc.perform(get("/api/v1/seller/products").with(actor(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(product.getId()))
                .andExpect(jsonPath("$[0].type").value("PHYSICAL"));
    }

    @Test
    void getProductReturns200() throws Exception {
        Product product = product();
        when(customerPort.getProduct(actor, product.getId())).thenReturn(product);

        mockMvc.perform(get("/api/v1/seller/products/{id}", product.getId())
                        .with(actor(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId()))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.type").value("PHYSICAL"));
    }

    @Test
    void updateProductReturns200() throws Exception {
        Product product = product();
        when(customerPort.updateProduct(any(), any(), any(), any(), any())).thenReturn(product);

        mockMvc.perform(put("/api/v1/seller/products/{id}", product.getId())
                        .with(actor(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Laptop Pro","description":"Updated","price":129.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId()))
                .andExpect(jsonPath("$.price.amount").value(119.00));
    }

    @Test
    void updateProductReturns404WhenMissing() throws Exception {
        when(customerPort.updateProduct(any(), any(), any(), any(), any()))
                .thenThrow(new ProductNotFoundException("missing"));

        mockMvc.perform(put("/api/v1/seller/products/{id}", "missing")
                        .with(actor(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Laptop Pro","description":"Updated","price":129.00}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void addVariantReturns200() throws Exception {
        Product product = product();
        product.addVariant(ProductVariant.of(VariantType.COLOR, "Red"));
        when(customerPort.addVariant(any(), any(), any(), any())).thenReturn(product);

        mockMvc.perform(post("/api/v1/seller/products/{id}/variants", product.getId())
                        .with(actor(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"COLOR","value":"Red"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId()))
                .andExpect(jsonPath("$.variants[0].type").value("COLOR"))
                .andExpect(jsonPath("$.variants[0].value").value("Red"));
    }

    @Test
    void publishProductReturns200() throws Exception {
        Product product = product();
        when(customerPort.publishProduct(any(), any())).thenReturn(product);

        mockMvc.perform(post("/api/v1/seller/products/{id}/publish", product.getId())
                        .with(actor(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void publishProductReturns409OnInvalidState() throws Exception {
        when(customerPort.publishProduct(any(), any()))
                .thenThrow(new InvalidProductStateException("cannot publish"));

        mockMvc.perform(post("/api/v1/seller/products/{id}/publish", "p1")
                        .with(actor(actor)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_PRODUCT_STATE"));
    }

    @Test
    void suspendProductReturns200() throws Exception {
        Product product = product();
        product.suspend();
        when(customerPort.suspendProduct(any(), any())).thenReturn(product);

        mockMvc.perform(post("/api/v1/seller/products/{id}/suspend", product.getId())
                        .with(actor(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    void discontinueProductReturns200() throws Exception {
        Product product = product();
        product.discontinue();
        when(customerPort.discontinueProduct(any(), any())).thenReturn(product);

        mockMvc.perform(post("/api/v1/seller/products/{id}/discontinue", product.getId())
                        .with(actor(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISCONTINUED"));
    }

    @Test
    void registerInventoryReturns201() throws Exception {
        Inventory inventory = inventory();
        when(customerPort.registerInventory(any(), any(), any(), anyInt())).thenReturn(inventory);

        mockMvc.perform(post("/api/v1/seller/inventory")
                        .with(actor(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"p1","warehouseId":"w1","initialQuantity":10}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(inventory.getId()))
                .andExpect(jsonPath("$.availableQuantity").value(10))
                .andExpect(jsonPath("$.condition").value("GOOD"));
    }

    @Test
    void registerInventoryRejectsBlankProductId() throws Exception {
        mockMvc.perform(post("/api/v1/seller/inventory")
                        .with(actor(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"","warehouseId":"w1","initialQuantity":10}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void listInventoryReturns200() throws Exception {
        Inventory inventory = inventory();
        when(customerPort.listInventory(actor)).thenReturn(List.of(inventory));

        mockMvc.perform(get("/api/v1/seller/inventory").with(actor(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(inventory.getId()))
                .andExpect(jsonPath("$[0].productId").value(inventory.getProduct().getId()));
    }

    @Test
    void listOrdersReturns200() throws Exception {
        Order order = order();
        when(customerPort.listOrders(actor)).thenReturn(List.of(order));

        mockMvc.perform(get("/api/v1/seller/orders").with(actor(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(order.getId()))
                .andExpect(jsonPath("$[0].status").value("PENDING_PAYMENT"));
    }

    private static Product product() {
        Seller seller = DomainFixtures.seller(DomainFixtures.admin());
        return DomainFixtures.publishedPhysicalProduct(seller, DomainFixtures.operator());
    }

    private static Inventory inventory() {
        Product product = product();
        Warehouse warehouse = product.getSeller().getWarehouses().get(0);
        return Inventory.create(product, warehouse, 10);
    }

    private static Order order() {
        Product product = product();
        Buyer buyer = DomainFixtures.buyer();
        Cart cart = DomainFixtures.cartWith(buyer, product, 2);
        return Order.fromCart(cart, DomainFixtures.address(), Instant.parse("2026-01-15T10:00:00Z"));
    }
}
