package application.domain.ports.in;

import application.domain.enums.UserRole;
import application.domain.enums.WarehouseType;
import application.domain.models.Refund;
import application.domain.models.Return;
import application.domain.models.Seller;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.valueobjects.Address;
import application.domain.valueobjects.Email;
import application.domain.valueobjects.FullName;
import application.domain.valueobjects.IdentityDocument;
import application.domain.valueobjects.Money;
import application.domain.valueobjects.TaxId;
import java.util.List;

public interface TellerEmployeePort {

    Seller registerSeller(User actor, String userId, TaxId taxId);

    List<Seller> listSellers(User actor);

    Seller updateSellerTaxId(User actor, String sellerId, TaxId taxId);

    Seller assignWarehouse(User actor, String sellerId, String warehouseId);

    Warehouse createWarehouse(User actor, String name, Address location,
                              WarehouseType type, String ownerSellerId,
                              String responsibleUserId);

    List<Warehouse> listWarehouses(User actor);

    Warehouse updateWarehouse(User actor, String warehouseId, String name,
                              Address location, String responsibleUserId);

    List<User> listUsers(User actor);

    User createUser(User actor, FullName fullName, Email email,
                    IdentityDocument identityDocument, UserRole role,
                    String rawPassword);

    User blockUser(User actor, String userId);

    User activateUser(User actor, String userId);

    User deactivateUser(User actor, String userId);

    List<Return> listReturns(User actor);

    Return approveReturn(User actor, String returnId);

    Return rejectReturn(User actor, String returnId);

    Refund generateRefund(User actor, String returnId, Money amount);

    Refund processRefund(User actor, String refundId);
}
