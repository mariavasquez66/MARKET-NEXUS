package application.domain.services;

import static application.domain.DomainFixtures.address;
import static application.domain.DomainFixtures.admin;
import static application.domain.DomainFixtures.buyer;
import static application.domain.DomainFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import application.domain.enums.CommercialStatus;
import application.domain.enums.UserRole;
import application.domain.exceptions.common.UnauthorizedOperationException;
import application.domain.models.Buyer;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.BuyerRepositoryPort;
import application.domain.ports.out.ClockPort;
import application.domain.valueobjects.Address;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BuyerServiceTest {
    @Mock BuyerRepositoryPort buyerRepository;
    @Mock AuditPort auditPort;
    @Mock ClockPort clockPort;

    private BuyerService service;

    @BeforeEach
    void setUp() {
        service = new BuyerService(buyerRepository, auditPort, clockPort);
        when(buyerRepository.save(any(Buyer.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void buyerCannotReadAnotherBuyer() {
        Buyer buyer = buyer();
        Buyer other = buyer();
        when(buyerRepository.findById(other.getId())).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> service.getBuyer(buyer.getUser(), other.getId()))
                .isInstanceOf(UnauthorizedOperationException.class);
        when(buyerRepository.findById(buyer.getId())).thenReturn(Optional.of(buyer));
        assertThat(service.getBuyer(buyer.getUser(), buyer.getId())).isSameAs(buyer);
    }

    @Test
    void adminCanRestrictAndReactivateBuyer() {
        Buyer buyer = buyer();
        when(buyerRepository.findById(buyer.getId())).thenReturn(Optional.of(buyer));
        service.restrict(admin(), buyer.getId());
        assertThat(buyer.getCommercialStatus()).isEqualTo(CommercialStatus.RESTRICTED);
        service.markEligible(admin(), buyer.getId());
        assertThat(buyer.getCommercialStatus()).isEqualTo(CommercialStatus.ELIGIBLE);
    }

    @Test
    void buyerCanManageOwnAddresses() {
        Buyer buyer = buyer();
        when(buyerRepository.findByUserId(buyer.getUser().getId())).thenReturn(Optional.of(buyer));
        Address second = Address.of("Calle 2", "Medellin", "Antioquia", "050001", "Colombia");
        service.addAdditionalAddress(buyer.getUser(), second);
        assertThat(service.listAddresses(buyer.getUser())).contains(second, buyer.getMainAddress());
        service.changeMainAddress(buyer.getUser(), second);
        assertThat(buyer.getMainAddress()).isEqualTo(second);
    }

    @Test
    void nonBuyerCannotUseBuyerService() {
        assertThatThrownBy(() -> service.getBuyerByUser(user(UserRole.SELLER)))
                .isInstanceOf(application.domain.exceptions.common.InsufficientPermissionException.class);
    }
}
