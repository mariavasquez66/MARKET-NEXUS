package application.domain.services;

import application.domain.enums.AuditAction;
import application.domain.enums.AuditEntityType;
import application.domain.enums.UserRole;
import application.domain.exceptions.common.DuplicateEntityException;
import application.domain.exceptions.common.UnauthorizedOperationException;
import application.domain.exceptions.seller.SellerNotFoundException;
import application.domain.exceptions.user.InvalidRoleException;
import application.domain.exceptions.user.UserNotFoundException;
import application.domain.exceptions.warehouse.WarehouseNotFoundException;
import application.domain.models.Seller;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.SellerRepositoryPort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.ports.out.WarehouseRepositoryPort;
import application.domain.valueobjects.TaxId;
import java.util.List;
import java.util.Objects;

public class SellerService {
    private final SellerRepositoryPort sellerRepository;
    private final UserRepositoryPort userRepository;
    private final WarehouseRepositoryPort warehouseRepository;
    private final AuditPort auditPort;
    private final ClockPort clockPort;

    public SellerService(SellerRepositoryPort sellerRepository,
                         UserRepositoryPort userRepository,
                         WarehouseRepositoryPort warehouseRepository,
                         AuditPort auditPort,
                         ClockPort clockPort) {
        this.sellerRepository = Objects.requireNonNull(sellerRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
        this.warehouseRepository = Objects.requireNonNull(warehouseRepository);
        this.auditPort = Objects.requireNonNull(auditPort);
        this.clockPort = Objects.requireNonNull(clockPort);
    }

    public Seller registerSeller(User actor, String userId, TaxId taxId) {
        User sellerUser = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        if (!sellerUser.hasRole(UserRole.SELLER)) {
            throw new InvalidRoleException("Seller user must have SELLER role");
        }
        Seller seller = Seller.registerBy(actor, sellerUser, taxId);
        if (sellerRepository.existsByTaxId(taxId)) {
            throw new DuplicateEntityException("Tax id is already registered");
        }
        Seller saved = sellerRepository.save(seller);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.SELLER_REGISTERED,
                AuditEntityType.SELLER, saved.getId(), actor, "Seller registered");
        return saved;
    }

    public Seller assignWarehouse(User actor, String sellerId, String warehouseId) {
        ServiceSupport.requireRole(actor, UserRole.ADMIN);
        Seller seller = requireSeller(sellerId);
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new WarehouseNotFoundException("Warehouse not found: " + warehouseId));
        seller.assignWarehouse(warehouse);
        warehouseRepository.save(warehouse);
        Seller saved = sellerRepository.save(seller);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.WAREHOUSE_ASSIGNED,
                AuditEntityType.WAREHOUSE, warehouse.getId(), actor, "Warehouse assigned to seller");
        return saved;
    }

    public Seller getSeller(User actor, String sellerId) {
        ServiceSupport.requireAnyRole(actor, UserRole.ADMIN, UserRole.SUPERVISOR, UserRole.SELLER);
        Seller seller = requireSeller(sellerId);
        if (!actor.hasRole(UserRole.ADMIN) && !actor.hasRole(UserRole.SUPERVISOR)
                && !seller.getUser().getId().equals(actor.getId())) {
            throw new UnauthorizedOperationException("Cannot access another seller");
        }
        return seller;
    }

    public List<Seller> listSellers(User actor) {
        ServiceSupport.requireAnyRole(actor, UserRole.ADMIN, UserRole.SUPERVISOR);
        return sellerRepository.findAll();
    }

    public Seller updateTaxId(User actor, String sellerId, TaxId taxId) {
        ServiceSupport.requireRole(actor, UserRole.ADMIN);
        Seller seller = requireSeller(sellerId);
        if (!seller.getTaxId().equals(taxId) && sellerRepository.existsByTaxId(taxId)) {
            throw new DuplicateEntityException("Tax id is already registered");
        }
        seller.updateTaxId(taxId);
        Seller saved = sellerRepository.save(seller);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.SELLER_TAX_ID_UPDATED,
                AuditEntityType.SELLER, saved.getId(), actor, "Seller tax id updated");
        return saved;
    }

    private Seller requireSeller(String sellerId) {
        return sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerNotFoundException("Seller not found: " + sellerId));
    }
}
