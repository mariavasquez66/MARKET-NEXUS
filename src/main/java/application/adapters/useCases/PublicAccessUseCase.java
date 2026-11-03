package application.adapters.useCases;

import application.domain.models.AuthResult;
import application.domain.models.Product;
import application.domain.models.User;
import application.domain.ports.in.PublicAccessPort;
import application.domain.services.AuthService;
import application.domain.services.CatalogService;
import application.domain.valueobjects.Address;
import application.domain.valueobjects.Email;
import application.domain.valueobjects.FullName;
import application.domain.valueobjects.IdentityDocument;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PublicAccessUseCase implements PublicAccessPort {

    private final AuthService authService;
    private final CatalogService catalogService;

    public PublicAccessUseCase(AuthService authService, CatalogService catalogService) {
        this.authService = authService;
        this.catalogService = catalogService;
    }

    @Override
    public List<Product> listPublishedProducts() {
        return catalogService.listPublishedProducts();
    }

    @Override
    public Product getPublishedProduct(String productId) {
        return catalogService.getPublishedProduct(productId);
    }

    @Override
    public User registerBuyer(FullName fullName,
                              Email email,
                              IdentityDocument identityDocument,
                              String rawPassword,
                              Address mainAddress) {
        return authService.registerBuyer(fullName, email, identityDocument, rawPassword, mainAddress);
    }

    @Override
    public AuthResult login(String email, String rawPassword) {
        return authService.login(email, rawPassword);
    }
}
