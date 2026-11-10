package application.domain.services;

import static application.domain.DomainFixtures.admin;
import static application.domain.DomainFixtures.buyer;
import static application.domain.DomainFixtures.operator;
import static application.domain.DomainFixtures.publishedPhysicalProduct;
import static application.domain.DomainFixtures.seller;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import application.domain.enums.AuditAction;
import application.domain.enums.UserRole;
import application.domain.exceptions.cart.CartNotFoundException;
import application.domain.exceptions.cart.ProductNotAvailableException;
import application.domain.exceptions.common.InvalidDataException;
import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.AuditLog;
import application.domain.models.Product;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.BuyerRepositoryPort;
import application.domain.ports.out.CartRepositoryPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.ProductRepositoryPort;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CartServiceTest {
    @Mock CartRepositoryPort cartRepository;
    @Mock BuyerRepositoryPort buyerRepository;
    @Mock ProductRepositoryPort productRepository;
    @Mock AuditPort auditPort;
    @Mock ClockPort clockPort;

    private CartService service;
    private Buyer buyer;

    @BeforeEach
    void setUp() {
        service = new CartService(cartRepository, buyerRepository, productRepository,
                auditPort, clockPort);
        buyer = buyer();
        when(clockPort.now()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        when(buyerRepository.findByUserId(buyer.getUser().getId())).thenReturn(Optional.of(buyer));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void getCartCreatesEmptyCartWhenMissing() {
        when(cartRepository.findByBuyerId(buyer.getId())).thenReturn(Optional.empty());
        Cart cart = service.getCart(buyer.getUser());
        assertThat(cart.getItems()).isEmpty();
        assertThat(cart.getBuyer()).isSameAs(buyer);
    }

    @Test
    void addItemRejectsUnpublishedProducts() {
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        product.suspend();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        assertThatThrownBy(() -> service.addItem(buyer.getUser(), product.getId(), 1))
                .isInstanceOf(ProductNotAvailableException.class);
    }

    @Test
    void updateAndRemoveRequireItemInCart() {
        when(cartRepository.findByBuyerId(buyer.getId())).thenReturn(Optional.of(Cart.create(buyer)));
        assertThatThrownBy(() -> service.updateQuantity(buyer.getUser(), "missing", 1))
                .isInstanceOf(InvalidDataException.class);
        assertThatThrownBy(() -> service.removeItem(buyer.getUser(), "missing"))
                .isInstanceOf(InvalidDataException.class);
    }

    @Test
    void updateRequiresExistingCart() {
        when(cartRepository.findByBuyerId(buyer.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateQuantity(buyer.getUser(), "p", 1))
                .isInstanceOf(CartNotFoundException.class);
    }

    @Test
    void recordsAuditForCartMutations() {
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(cartRepository.findByBuyerId(buyer.getId())).thenReturn(Optional.empty());

        service.addItem(buyer.getUser(), product.getId(), 2);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditPort).record(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.CART_ITEM_ADDED);
        assertThat(captor.getValue().getEntityType().getCode()).isEqualTo("CART");
        assertThat(captor.getValue().getDetails()).contains(product.getId()).contains("x2");
    }

    @Test
    void nonBuyerCannotUseCart() {
        assertThatThrownBy(() -> service.getCart(application.domain.DomainFixtures.user(UserRole.SELLER)))
                .isInstanceOf(application.domain.exceptions.common.InsufficientPermissionException.class);
    }
}
