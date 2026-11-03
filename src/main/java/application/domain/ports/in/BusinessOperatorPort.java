package application.domain.ports.in;

import application.domain.enums.OrderStatus;
import application.domain.models.Inventory;
import application.domain.models.InventoryMovement;
import application.domain.models.Order;
import application.domain.models.Return;
import application.domain.models.Shipment;
import application.domain.models.User;
import java.util.List;

public interface BusinessOperatorPort {

    List<Order> listOrders(User actor, OrderStatus status);

    Shipment createShipment(User actor, String orderId, String warehouseId);

    Shipment startShipping(User actor, String shipmentId, String trackingNumber);

    Shipment confirmDelivery(User actor, String shipmentId);

    InventoryMovement registerInflow(User actor, String productId,
                                     String warehouseId, int quantity);

    InventoryMovement adjustInventory(User actor, String productId,
                                      String warehouseId, int delta);

    Inventory markDamaged(User actor, String productId, String warehouseId);

    Inventory markRepaired(User actor, String productId, String warehouseId);

    List<Return> listReturns(User actor);

    Return receiveReturn(User actor, String returnId);
}
