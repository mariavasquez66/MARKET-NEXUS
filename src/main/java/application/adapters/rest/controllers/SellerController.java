package application.adapters.rest.controllers;

import application.adapters.rest.dto.request.AddVariantRequest;
import application.adapters.rest.dto.request.CreateProductRequest;
import application.adapters.rest.dto.request.RegisterInventoryRequest;
import application.adapters.rest.dto.request.UpdateProductRequest;
import application.adapters.rest.dto.response.InventoryResponse;
import application.adapters.rest.dto.response.OrderResponse;
import application.adapters.rest.dto.response.ProductResponse;
import application.adapters.rest.mappers.InventoryRestMapper;
import application.adapters.rest.mappers.OrderRestMapper;
import application.adapters.rest.mappers.ProductRestMapper;
import application.domain.enums.ProductType;
import application.domain.enums.VariantType;
import application.domain.models.Inventory;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.User;
import application.domain.ports.in.BusinessCustomerPort;
import application.domain.valueobjects.Money;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seller")
@Validated
public class SellerController {

    private final BusinessCustomerPort customerPort;
    private final ProductRestMapper productRestMapper;
    private final InventoryRestMapper inventoryRestMapper;
    private final OrderRestMapper orderRestMapper;

    public SellerController(BusinessCustomerPort customerPort,
                            ProductRestMapper productRestMapper,
                            InventoryRestMapper inventoryRestMapper,
                            OrderRestMapper orderRestMapper) {
        this.customerPort = customerPort;
        this.productRestMapper = productRestMapper;
        this.inventoryRestMapper = inventoryRestMapper;
        this.orderRestMapper = orderRestMapper;
    }

    @PostMapping("/products")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request,
            @AuthenticationPrincipal(expression = "user") User actor) {
        Product product = customerPort.registerProduct(
                actor,
                request.name(),
                request.description(),
                ProductType.fromCode(request.type()),
                Money.of(request.price()));
        return ResponseEntity.status(HttpStatus.CREATED).body(productRestMapper.toResponse(product));
    }

    @GetMapping("/products")
    public List<ProductResponse> listProducts(@AuthenticationPrincipal(expression = "user") User actor) {
        return customerPort.listProducts(actor).stream()
                .map(productRestMapper::toResponse)
                .toList();
    }

    @PutMapping("/products/{id}")
    public ProductResponse updateProduct(@PathVariable String id,
                                         @Valid @RequestBody UpdateProductRequest request,
                                         @AuthenticationPrincipal(expression = "user") User actor) {
        Product product = customerPort.updateProduct(
                actor,
                id,
                request.name(),
                request.description(),
                Money.of(request.price()));
        return productRestMapper.toResponse(product);
    }

    @PostMapping("/products/{id}/variants")
    public ProductResponse addVariant(@PathVariable String id,
                                      @Valid @RequestBody AddVariantRequest request,
                                      @AuthenticationPrincipal(expression = "user") User actor) {
        Product product = customerPort.addVariant(
                actor,
                id,
                VariantType.fromCode(request.type()),
                request.value());
        return productRestMapper.toResponse(product);
    }

    @PostMapping("/products/{id}/publish")
    public ProductResponse publishProduct(@PathVariable String id,
                                          @AuthenticationPrincipal(expression = "user") User actor) {
        return productRestMapper.toResponse(customerPort.publishProduct(actor, id));
    }

    @PostMapping("/products/{id}/suspend")
    public ProductResponse suspendProduct(@PathVariable String id,
                                          @AuthenticationPrincipal(expression = "user") User actor) {
        return productRestMapper.toResponse(customerPort.suspendProduct(actor, id));
    }

    @PostMapping("/products/{id}/discontinue")
    public ProductResponse discontinueProduct(@PathVariable String id,
                                              @AuthenticationPrincipal(expression = "user") User actor) {
        return productRestMapper.toResponse(customerPort.discontinueProduct(actor, id));
    }

    @PostMapping("/inventory")
    public ResponseEntity<InventoryResponse> registerInventory(
            @Valid @RequestBody RegisterInventoryRequest request,
            @AuthenticationPrincipal(expression = "user") User actor) {
        Inventory inventory = customerPort.registerInventory(
                actor,
                request.productId(),
                request.warehouseId(),
                request.initialQuantity());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventoryRestMapper.toResponse(inventory));
    }

    @GetMapping("/inventory")
    public List<InventoryResponse> listInventory(@AuthenticationPrincipal(expression = "user") User actor) {
        return customerPort.listInventory(actor).stream()
                .map(inventoryRestMapper::toResponse)
                .toList();
    }

    @GetMapping("/orders")
    public List<OrderResponse> listOrders(@AuthenticationPrincipal(expression = "user") User actor) {
        return customerPort.listOrders(actor).stream()
                .map(orderRestMapper::toResponse)
                .toList();
    }
}
