package application.adapters.rest.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import application.adapters.rest.mappers.ProductRestMapper;
import application.adapters.rest.mappers.UserRestMapper;
import application.domain.DomainFixtures;
import application.domain.enums.UserRole;
import application.domain.exceptions.product.ProductNotFoundException;
import application.domain.exceptions.common.UnauthenticatedUserException;
import application.domain.models.AuthResult;
import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.models.User;
import application.domain.ports.in.PublicAccessPort;
import application.domain.valueobjects.Address;
import application.domain.valueobjects.Email;
import application.domain.valueobjects.FullName;
import application.domain.valueobjects.IdentityDocument;
import application.infrastructure.exception.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PublicControllerTest {

    @Mock
    private PublicAccessPort publicAccessPort;

    private final UserRestMapper userRestMapper = new UserRestMapper();
    private final ProductRestMapper productRestMapper = new ProductRestMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PublicController controller =
                new PublicController(publicAccessPort, userRestMapper, productRestMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(RestControllerTestSupport.actorResolver())
                .build();
    }

    @Test
    void listProductsReturnsPublishedCatalog() throws Exception {
        Product product = publishedProduct();
        when(publicAccessPort.listPublishedProducts()).thenReturn(List.of(product));

        mockMvc.perform(get("/api/v1/public/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(product.getId()))
                .andExpect(jsonPath("$[0].name").value(product.getName()))
                .andExpect(jsonPath("$[0].type").value("PHYSICAL"))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$[0].price.currency").value("COP"));
    }

    @Test
    void getProductReturnsDetail() throws Exception {
        Product product = publishedProduct();
        when(publicAccessPort.getPublishedProduct(product.getId())).thenReturn(product);

        mockMvc.perform(get("/api/v1/public/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId()))
                .andExpect(jsonPath("$.sellerId").value(product.getSeller().getId()))
                .andExpect(jsonPath("$.name").value(product.getName()));
    }

    @Test
    void registerBuyerReturnsCreatedUserAndMapsValueObjects() throws Exception {
        User user = DomainFixtures.user(UserRole.BUYER);
        when(publicAccessPort.registerBuyer(any(), any(), any(), any(), any())).thenReturn(user);

        String body = """
                {
                  "fullName": "Jane Buyer",
                  "email": "jane.buyer@example.com",
                  "identityDocument": "DOC12345",
                  "password": "secret123",
                  "mainAddress": {
                    "street": "Calle 1 # 2-3",
                    "city": "Bogota",
                    "region": "Cundinamarca",
                    "postalCode": "110111",
                    "country": "Colombia"
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/public/buyers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value(user.getEmail().getValue()))
                .andExpect(jsonPath("$.role").value("BUYER"));

        ArgumentCaptor<FullName> fullName = ArgumentCaptor.forClass(FullName.class);
        ArgumentCaptor<Email> email = ArgumentCaptor.forClass(Email.class);
        ArgumentCaptor<IdentityDocument> document = ArgumentCaptor.forClass(IdentityDocument.class);
        ArgumentCaptor<String> password = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Address> address = ArgumentCaptor.forClass(Address.class);
        verify(publicAccessPort).registerBuyer(
                fullName.capture(), email.capture(), document.capture(),
                password.capture(), address.capture());

        assertThat(fullName.getValue().getValue()).isEqualTo("Jane Buyer");
        assertThat(email.getValue().getValue()).isEqualTo("jane.buyer@example.com");
        assertThat(document.getValue().getValue()).isEqualTo("DOC12345");
        assertThat(password.getValue()).isEqualTo("secret123");
        assertThat(address.getValue().getCity()).isEqualTo("Bogota");
        assertThat(address.getValue().getPostalCode()).isEqualTo("110111");
    }

    @Test
    void loginReturnsTokenAndUser() throws Exception {
        User user = DomainFixtures.user(UserRole.BUYER);
        when(publicAccessPort.login(user.getEmail().getValue(), "secret123"))
                .thenReturn(new AuthResult("jwt-token-123", user));

        String body = """
                {
                  "email": "%s",
                  "password": "secret123"
                }
                """.formatted(user.getEmail().getValue());

        mockMvc.perform(post("/api/v1/public/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.user.id").value(user.getId()))
                .andExpect(jsonPath("$.user.email").value(user.getEmail().getValue()))
                .andExpect(jsonPath("$.user.role").value("BUYER"));
    }

    @Test
    void registerBuyerRejectsInvalidBody() throws Exception {
        String body = """
                {
                  "fullName": "Jane Buyer",
                  "email": "not-an-email",
                  "identityDocument": "DOC12345",
                  "password": "secret123",
                  "mainAddress": {
                    "street": "Calle 1",
                    "city": "Bogota",
                    "region": "Cundinamarca",
                    "country": "Colombia"
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/public/buyers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void getMissingProductReturnsNotFound() throws Exception {
        when(publicAccessPort.getPublishedProduct("missing"))
                .thenThrow(new ProductNotFoundException("Product not found"));

        mockMvc.perform(get("/api/v1/public/products/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void loginWithInvalidCredentialsReturnsUnauthorized() throws Exception {
        when(publicAccessPort.login("jane.buyer@example.com", "wrong"))
                .thenThrow(new UnauthenticatedUserException("Invalid credentials"));

        String body = """
                {
                  "email": "jane.buyer@example.com",
                  "password": "wrong"
                }
                """;

        mockMvc.perform(post("/api/v1/public/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    private static Product publishedProduct() {
        Seller seller = DomainFixtures.seller(DomainFixtures.admin());
        return DomainFixtures.publishedPhysicalProduct(seller, DomainFixtures.operator());
    }
}
