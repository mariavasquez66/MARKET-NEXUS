package application.domain.ports.in;

import application.domain.models.AuthResult;
import application.domain.models.Product;
import application.domain.models.User;
import application.domain.valueobjects.Address;
import application.domain.valueobjects.Email;
import application.domain.valueobjects.FullName;
import application.domain.valueobjects.IdentityDocument;
import java.util.List;

public interface PublicAccessPort {

    List<Product> listPublishedProducts();

    Product getPublishedProduct(String productId);

    User registerBuyer(FullName fullName,
                       Email email,
                       IdentityDocument identityDocument,
                       String rawPassword,
                       Address mainAddress);

    AuthResult login(String email, String rawPassword);
}
