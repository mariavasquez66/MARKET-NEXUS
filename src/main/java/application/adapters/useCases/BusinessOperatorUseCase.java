package application.adapters.useCases;

import application.domain.enums.OrderStatus;
import application.domain.models.Inventory;
import application.domain.models.InventoryMovement;
import application.domain.models.Order;
import application.domain.models.Return;
import application.domain.models.Shipment;
import application.domain.models.User;
import application.domain.ports.in.BusinessOperatorPort;
import application.domain.services.InventoryService;
import application.domain.services.LogisticsService;
import application.domain.services.OrderService;
import application.domain.services.ReturnService;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BusinessOperatorUseCase implements BusinessOperatorPort {

    private final InventoryService inventoryService;
    private final LogisticsService logisticsService;
    private final OrderService orderService;
    private final ReturnService returnService;

    public BusinessOperatorUseCase(InventoryService inventoryService,
                                   LogisticsService logisticsService,
                                   OrderService orderService,
                                   ReturnService returnService) {
        this.inventoryService = inventoryService;
        this.logisticsService = logisticsService;
        this.orderService = orderService;
        this.returnService = returnService;
    }

    @Override
    public List<Order> listOrders(User actor, OrderStatus status) {
        return status == null ? orderService.listOrders(actor) : orderService.listByStatus(actor, status);
    }

    @Override
    public Shipment createShipment(User actor, String orderId, String warehouseId) {
        return logisticsService.createShipment(actor, orderId, warehouseId);
    }

    @Override
    public Shipment startShipping(User actor, String shipmentId, String trackingNumber) {
        return logisticsService.startShipping(actor, shipmentId, trackingNumber);
    }

    @Override
    public Shipment confirmDelivery(User actor, String shipmentId) {
        return logisticsService.confirmDelivery(actor, shipmentId);
    }

    @Override
    public InventoryMovement registerInflow(User actor, String productId,
                                            String warehouseId, int quantity) {
        return inventoryService.addStock(actor, productId, warehouseId, quantity);
    }

    @Override
    public InventoryMovement adjustInventory(User actor, String productId,
                                             String warehouseId, int delta) {
        return inventoryService.adjust(actor, productId, warehouseId, delta);
    }

    @Override
    public Inventory markDamaged(User actor, String productId, String warehouseId) {
        return inventoryService.markDamaged(actor, productId, warehouseId);
    }

    @Override
    public Inventory markRepaired(User actor, String productId, String warehouseId) {
        return inventoryService.markRepaired(actor, productId, warehouseId);
    }

    @Override
    public List<Return> listReturns(User actor) {
        return returnService.listReturns(actor);
    }

    @Override
    public Return receiveReturn(User actor, String returnId) {
        return returnService.complete(actor, returnId);
    }
}
