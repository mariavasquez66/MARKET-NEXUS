package application.domain.services;

import static application.domain.DomainFixtures.address;
import static application.domain.DomainFixtures.admin;
import static application.domain.DomainFixtures.operator;
import static application.domain.DomainFixtures.seller;
import static application.domain.DomainFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import application.domain.enums.UserRole;
import application.domain.exceptions.common.DuplicateEntityException;
import application.domain.exceptions.seller.SelfRegistrationNotAllowedException;
import application.domain.exceptions.user.InvalidRoleException;
import application.domain.models.Seller;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.SellerRepositoryPort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.ports.out.WarehouseRepositoryPort;
import application.domain.valueobjects.TaxId;
import java.time.Instant;
import java.util.List;
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
class SellerServiceTest {
    @Mock SellerRepositoryPort sellerRepository;
    @Mock UserRepositoryPort userRepository;
    @Mock WarehouseRepositoryPort warehouseRepository;
    @Mock AuditPort auditPort;
    @Mock ClockPort clockPort;

    private SellerService service;

    @BeforeEach
    void setUp() {
        service = new SellerService(sellerRepository, userRepository, warehouseRepository,
                auditPort, clockPort);
        when(clockPort.now()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void onlyAdminCanRegisterSellers() {
        User sellerUser = user(UserRole.SELLER);
        when(userRepository.findById(sellerUser.getId())).thenReturn(Optional.of(sellerUser));
        assertThatThrownBy(() -> service.registerSeller(user(UserRole.BUYER), sellerUser.getId(),
                TaxId.of("900123456-7")))
                .isInstanceOf(SelfRegistrationNotAllowedException.class);
    }

    @Test
    void sellerUserMustHaveSellerRole() {
        User buyerUser = user(UserRole.BUYER);
        when(userRepository.findById(buyerUser.getId())).thenReturn(Optional.of(buyerUser));
        assertThatThrownBy(() -> service.registerSeller(admin(), buyerUser.getId(),
                TaxId.of("900123456-7")))
                .isInstanceOf(InvalidRoleException.class);
    }

    @Test
    void duplicateTaxIdIsRejected() {
        User sellerUser = user(UserRole.SELLER);
        TaxId taxId = TaxId.of("900123456-7");
        when(userRepository.findById(sellerUser.getId())).thenReturn(Optional.of(sellerUser));
        when(sellerRepository.existsByTaxId(taxId)).thenReturn(true);
        assertThatThrownBy(() -> service.registerSeller(admin(), sellerUser.getId(), taxId))
                .isInstanceOf(DuplicateEntityException.class);
    }

    @Test
    void registerSellerSavesAndAudits() {
        User sellerUser = user(UserRole.SELLER);
        when(userRepository.findById(sellerUser.getId())).thenReturn(Optional.of(sellerUser));
        Seller seller = service.registerSeller(admin(), sellerUser.getId(), TaxId.of("900123456-7"));
        assertThat(seller.getUser()).isSameAs(sellerUser);
        org.mockito.Mockito.verify(auditPort).record(any());
    }

    @Test
    void assignWarehouseValidatesOwnership() {
        Seller seller = seller(admin());
        Warehouse warehouse = Warehouse.createSellerWarehouse("W", address(), seller, operator());
        when(sellerRepository.findById(seller.getId())).thenReturn(Optional.of(seller));
        when(warehouseRepository.findById(warehouse.getId())).thenReturn(Optional.of(warehouse));
        Seller result = service.assignWarehouse(admin(), seller.getId(), warehouse.getId());
        assertThat(result.getWarehouses()).hasSize(1);
    }

    @Test
    void listSellersIsRestrictedToAdminAndSupervisor() {
        assertThatThrownBy(() -> service.listSellers(user(UserRole.SELLER)))
                .isInstanceOf(application.domain.exceptions.common.InsufficientPermissionException.class);
        when(sellerRepository.findAll()).thenReturn(List.of());
        assertThat(service.listSellers(admin())).isEmpty();
    }
}
