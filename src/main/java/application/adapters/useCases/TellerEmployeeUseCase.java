package application.adapters.useCases;

import application.domain.enums.UserRole;
import application.domain.enums.WarehouseType;
import application.domain.models.Refund;
import application.domain.models.Return;
import application.domain.models.Seller;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.ports.in.TellerEmployeePort;
import application.domain.services.RefundService;
import application.domain.services.ReturnService;
import application.domain.services.SellerService;
import application.domain.services.UserService;
import application.domain.services.WarehouseService;
import application.domain.valueobjects.Address;
import application.domain.valueobjects.Email;
import application.domain.valueobjects.FullName;
import application.domain.valueobjects.IdentityDocument;
import application.domain.valueobjects.Money;
import application.domain.valueobjects.TaxId;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TellerEmployeeUseCase implements TellerEmployeePort {

    private final SellerService sellerService;
    private final WarehouseService warehouseService;
    private final UserService userService;
    private final ReturnService returnService;
    private final RefundService refundService;

    public TellerEmployeeUseCase(SellerService sellerService,
                                 WarehouseService warehouseService,
                                 UserService userService,
                                 ReturnService returnService,
                                 RefundService refundService) {
        this.sellerService = sellerService;
        this.warehouseService = warehouseService;
        this.userService = userService;
        this.returnService = returnService;
        this.refundService = refundService;
    }

    @Override
    public Seller registerSeller(User actor, String userId, TaxId taxId) {
        return sellerService.registerSeller(actor, userId, taxId);
    }

    @Override
    public List<Seller> listSellers(User actor) {
        return sellerService.listSellers(actor);
    }

    @Override
    public Seller updateSellerTaxId(User actor, String sellerId, TaxId taxId) {
        return sellerService.updateTaxId(actor, sellerId, taxId);
    }

    @Override
    public Seller assignWarehouse(User actor, String sellerId, String warehouseId) {
        return sellerService.assignWarehouse(actor, sellerId, warehouseId);
    }

    @Override
    public Warehouse createWarehouse(User actor, String name, Address location,
                                     WarehouseType type, String ownerSellerId,
                                     String responsibleUserId) {
        return warehouseService.createWarehouse(actor, name, location, type,
                ownerSellerId, responsibleUserId);
    }

    @Override
    public List<Warehouse> listWarehouses(User actor) {
        return warehouseService.listWarehouses(actor);
    }

    @Override
    public Warehouse updateWarehouse(User actor, String warehouseId, String name,
                                     Address location, String responsibleUserId) {
        return warehouseService.updateWarehouse(actor, warehouseId, name, location, responsibleUserId);
    }

    @Override
    public List<User> listUsers(User actor) {
        return userService.listUsers(actor);
    }

    @Override
    public User createUser(User actor, FullName fullName, Email email,
                           IdentityDocument identityDocument, UserRole role,
                           String rawPassword) {
        return userService.createUser(actor, fullName, email, identityDocument, role, rawPassword);
    }

    @Override
    public User blockUser(User actor, String userId) {
        return userService.blockUser(actor, userId);
    }

    @Override
    public User activateUser(User actor, String userId) {
        return userService.activateUser(actor, userId);
    }

    @Override
    public User deactivateUser(User actor, String userId) {
        return userService.deactivateUser(actor, userId);
    }

    @Override
    public List<Return> listReturns(User actor) {
        return returnService.listReturns(actor);
    }

    @Override
    public Return approveReturn(User actor, String returnId) {
        return returnService.approve(actor, returnId);
    }

    @Override
    public Return rejectReturn(User actor, String returnId) {
        return returnService.reject(actor, returnId);
    }

    @Override
    public Refund generateRefund(User actor, String returnId, Money amount) {
        return refundService.generateRefund(actor, returnId, amount);
    }

    @Override
    public Refund processRefund(User actor, String refundId) {
        return refundService.process(actor, refundId);
    }
}
