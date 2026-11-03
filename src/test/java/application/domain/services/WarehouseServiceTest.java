package application.domain.services;

import static application.domain.DomainFixtures.address;
import static application.domain.DomainFixtures.admin;
import static application.domain.DomainFixtures.buyer;
import static application.domain.DomainFixtures.operator;
import static application.domain.DomainFixtures.seller;
import static application.domain.DomainFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import application.domain.enums.UserRole;
import application.domain.enums.WarehouseType;
import application.domain.exceptions.user.InvalidRoleException;
import application.domain.exceptions.warehouse.InvalidWarehouseTypeException;
import application.domain.models.Seller;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.SellerRepositoryPort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.ports.out.WarehouseRepositoryPort;
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
class WarehouseServiceTest {
    @Mock WarehouseRepositoryPort warehouseRepository;
    @Mock SellerRepositoryPort sellerRepository;
    @Mock UserRepositoryPort userRepository;
    @Mock AuditPort auditPort;
    @Mock ClockPort clockPort;

    private WarehouseService service;

    @BeforeEach
    void setUp() {
        service = new WarehouseService(warehouseRepository, sellerRepository,
                userRepository, auditPort, clockPort);
        when(clockPort.now()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void responsibleMustHaveAllowedRole() {
        User buyerUser = user(UserRole.BUYER);
        when(userRepository.findById(buyerUser.getId())).thenReturn(Optional.of(buyerUser));
        assertThatThrownBy(() -> service.createWarehouse(admin(), "W", address(),
                WarehouseType.MARKETPLACE, null, buyerUser.getId()))
                .isInstanceOf(InvalidRoleException.class);
    }

    @Test
    void marketplaceRejectsOwnerAndSellerRequiresOwner() {
        User responsible = operator();
        when(userRepository.findById(responsible.getId())).thenReturn(Optional.of(responsible));
        assertThatThrownBy(() -> service.createWarehouse(admin(), "W", address(),
                WarehouseType.MARKETPLACE, "seller-id", responsible.getId()))
                .isInstanceOf(InvalidWarehouseTypeException.class);
        assertThatThrownBy(() -> service.createWarehouse(admin(), "W", address(),
                WarehouseType.SELLER, null, responsible.getId()))
                .isInstanceOf(InvalidWarehouseTypeException.class);
    }

    @Test
    void createsMarketplaceAndSellerWarehouses() {
        User responsible = operator();
        when(userRepository.findById(responsible.getId())).thenReturn(Optional.of(responsible));
        Warehouse marketplace = service.createWarehouse(admin(), "Central", address(),
                WarehouseType.MARKETPLACE, null, responsible.getId());
        assertThat(marketplace.getType()).isEqualTo(WarehouseType.MARKETPLACE);

        Seller seller = seller(admin());
        when(sellerRepository.findById(seller.getId())).thenReturn(Optional.of(seller));
        Warehouse owned = service.createWarehouse(admin(), "Seller", address(),
                WarehouseType.SELLER, seller.getId(), responsible.getId());
        assertThat(owned.getOwner()).isSameAs(seller);
    }

    @Test
    void updateWarehouseValidatesResponsible() {
        Warehouse warehouse = Warehouse.createMarketplace("Central", address(), operator());
        when(warehouseRepository.findById(warehouse.getId())).thenReturn(Optional.of(warehouse));
        User buyerUser = user(UserRole.BUYER);
        when(userRepository.findById(buyerUser.getId())).thenReturn(Optional.of(buyerUser));
        assertThatThrownBy(() -> service.updateWarehouse(admin(), warehouse.getId(), "New",
                address(), buyerUser.getId())).isInstanceOf(InvalidRoleException.class);
    }

    @Test
    void sellerOnlySeesOwnWarehouses() {
        Seller seller = seller(admin());
        when(sellerRepository.findByUserId(seller.getUser().getId())).thenReturn(Optional.of(seller));
        when(warehouseRepository.findByOwnerSellerId(seller.getId())).thenReturn(List.of());
        assertThat(service.listWarehouses(seller.getUser())).isEmpty();
        assertThatThrownBy(() -> service.listWarehouses(buyer().getUser()))
                .isInstanceOf(application.domain.exceptions.common.InsufficientPermissionException.class);
    }
}
