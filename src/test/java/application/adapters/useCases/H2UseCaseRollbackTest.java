package application.adapters.useCases;

import static application.domain.DomainFixtures.address;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import application.adapters.persistence.jpa.adapters.BuyerJpaAdapter;
import application.adapters.persistence.jpa.adapters.JpaDependencyResolver;
import application.adapters.persistence.jpa.adapters.UserJpaAdapter;
import application.adapters.persistence.jpa.mappers.BuyerJpaMapper;
import application.adapters.persistence.jpa.mappers.CartJpaMapper;
import application.adapters.persistence.jpa.mappers.InventoryJpaMapper;
import application.adapters.persistence.jpa.mappers.InvoiceJpaMapper;
import application.adapters.persistence.jpa.mappers.OrderJpaMapper;
import application.adapters.persistence.jpa.mappers.ProductJpaMapper;
import application.adapters.persistence.jpa.mappers.ReturnJpaMapper;
import application.adapters.persistence.jpa.mappers.SellerJpaMapper;
import application.adapters.persistence.jpa.mappers.UserJpaMapper;
import application.adapters.persistence.jpa.mappers.WarehouseJpaMapper;
import application.adapters.persistence.jpa.repositories.BuyerJpaRepository;
import application.adapters.persistence.jpa.repositories.UserJpaRepository;
import application.domain.models.AuditLog;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.JwtServicePort;
import application.domain.ports.out.PasswordServicePort;
import application.domain.services.AuthService;
import application.domain.services.CatalogService;
import application.domain.valueobjects.Email;
import application.domain.valueobjects.FullName;
import application.domain.valueobjects.IdentityDocument;
import application.domain.valueobjects.PasswordHash;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import({
        JpaDependencyResolver.class,
        UserJpaMapper.class,
        BuyerJpaMapper.class,
        SellerJpaMapper.class,
        WarehouseJpaMapper.class,
        ProductJpaMapper.class,
        InventoryJpaMapper.class,
        CartJpaMapper.class,
        OrderJpaMapper.class,
        InvoiceJpaMapper.class,
        ReturnJpaMapper.class,
        UserJpaAdapter.class,
        BuyerJpaAdapter.class,
        AuthService.class,
        PublicAccessUseCase.class
})
class H2UseCaseRollbackTest {

    @Autowired
    private PublicAccessUseCase publicAccessUseCase;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private BuyerJpaRepository buyerRepository;

    @MockitoBean
    private AuditPort auditPort;

    @MockitoBean
    private ClockPort clockPort;

    @MockitoBean
    private PasswordServicePort passwordService;

    @MockitoBean
    private JwtServicePort jwtService;

    @MockitoBean
    private CatalogService catalogService;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void registerBuyerRollsBackEveryWriteWhenAuditFails() {
        when(clockPort.now()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        when(passwordService.encode(anyString())).thenReturn(PasswordHash.of("$2a$10$rollback"));
        doThrow(new IllegalStateException("audit unavailable"))
                .when(auditPort).record(any(AuditLog.class));

        Email email = Email.of("rolled-back@nexusmarket.test");
        IdentityDocument identityDocument = IdentityDocument.of("DOC-ROLLBACK-1");

        assertThatThrownBy(() -> publicAccessUseCase.registerBuyer(
                FullName.of("Rollback Buyer"),
                email,
                identityDocument,
                "Password1",
                address()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("audit unavailable");

        assertThat(userRepository.existsByEmailValue(email.getValue())).isFalse();
        assertThat(userRepository.existsByIdentityDocumentValue(identityDocument.getValue()))
                .isFalse();
        assertThat(buyerRepository.findAll()).isEmpty();
    }
}
