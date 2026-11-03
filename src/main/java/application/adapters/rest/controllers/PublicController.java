package application.adapters.rest.controllers;

import application.adapters.rest.dto.request.LoginRequest;
import application.adapters.rest.dto.request.RegisterBuyerRequest;
import application.adapters.rest.dto.response.AuthResponse;
import application.adapters.rest.dto.response.ProductResponse;
import application.adapters.rest.dto.response.UserResponse;
import application.adapters.rest.mappers.ProductRestMapper;
import application.adapters.rest.mappers.UserRestMapper;
import application.domain.models.AuthResult;
import application.domain.models.Product;
import application.domain.models.User;
import application.domain.ports.in.PublicAccessPort;
import application.domain.valueobjects.Address;
import application.domain.valueobjects.Email;
import application.domain.valueobjects.FullName;
import application.domain.valueobjects.IdentityDocument;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
@Validated
public class PublicController {

    private final PublicAccessPort publicAccessPort;
    private final UserRestMapper userRestMapper;
    private final ProductRestMapper productRestMapper;

    public PublicController(PublicAccessPort publicAccessPort,
                            UserRestMapper userRestMapper,
                            ProductRestMapper productRestMapper) {
        this.publicAccessPort = publicAccessPort;
        this.userRestMapper = userRestMapper;
        this.productRestMapper = productRestMapper;
    }

    @GetMapping("/products")
    public List<ProductResponse> listProducts() {
        return publicAccessPort.listPublishedProducts().stream()
                .map(productRestMapper::toResponse)
                .toList();
    }

    @GetMapping("/products/{id}")
    public ProductResponse getProduct(@PathVariable String id) {
        return productRestMapper.toResponse(publicAccessPort.getPublishedProduct(id));
    }

    @PostMapping("/buyers/register")
    public ResponseEntity<UserResponse> registerBuyer(
            @Valid @RequestBody RegisterBuyerRequest request) {
        Address mainAddress = Address.of(
                request.mainAddress().street(),
                request.mainAddress().city(),
                request.mainAddress().region(),
                request.mainAddress().postalCode(),
                request.mainAddress().country());
        User user = publicAccessPort.registerBuyer(
                FullName.of(request.fullName()),
                Email.of(request.email()),
                IdentityDocument.of(request.identityDocument()),
                request.password(),
                mainAddress);
        return ResponseEntity.status(HttpStatus.CREATED).body(userRestMapper.toResponse(user));
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = publicAccessPort.login(request.email(), request.password());
        return userRestMapper.toResponse(result);
    }
}
