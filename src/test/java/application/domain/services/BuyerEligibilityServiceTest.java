package application.domain.services;

import static application.domain.DomainFixtures.admin;
import static application.domain.DomainFixtures.buyer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import application.domain.exceptions.common.InsufficientPermissionException;
import application.domain.models.Buyer;
import application.domain.ports.out.BuyerRepositoryPort;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BuyerEligibilityServiceTest {

    @Mock BuyerRepositoryPort buyerRepository;

    private BuyerEligibilityService service;

    @BeforeEach
    void setUp() {
        service = new BuyerEligibilityService(buyerRepository);
    }

    @Test
    void adminCanRestrictAndReactivateBuyer() {
        Buyer buyer = buyer();
        when(buyerRepository.findById(buyer.getId())).thenReturn(Optional.of(buyer));
        when(buyerRepository.save(any(Buyer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.restrict(admin(), buyer.getId());
        assertThat(buyer.getCommercialStatus().getCode()).isEqualTo("RESTRICTED");

        service.markEligible(admin(), buyer.getId());
        assertThat(buyer.getCommercialStatus().getCode()).isEqualTo("ELIGIBLE");
    }

    @Test
    void nonAdminCannotChangeBuyerEligibility() {
        Buyer buyer = buyer();

        assertThatThrownBy(() -> service.restrict(buyer.getUser(), buyer.getId()))
                .isInstanceOf(InsufficientPermissionException.class);
    }
}
