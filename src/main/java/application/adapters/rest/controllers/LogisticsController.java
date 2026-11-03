package application.adapters.rest.controllers;

import application.adapters.rest.dto.request.CreateShipmentRequest;
import application.adapters.rest.dto.request.InventoryAdjustmentRequest;
import application.adapters.rest.dto.request.StartShipmentRequest;
import application.adapters.rest.dto.response.InventoryMovementResponse;
import application.adapters.rest.dto.response.InventoryResponse;
import application.adapters.rest.dto.response.OrderResponse;
import application.adapters.rest.dto.response.ReturnResponse;
import application.adapters.rest.dto.response.ShipmentResponse;
import application.adapters.rest.mappers.InventoryMovementRestMapper;
import application.adapters.rest.mappers.InventoryRestMapper;
import application.adapters.rest.mappers.OrderRestMapper;
import application.adapters.rest.mappers.ReturnRestMapper;
import application.adapters.rest.mappers.ShipmentRestMapper;
import application.domain.enums.OrderStatus;
import application.domain.exceptions.common.InvalidDataException;
import application.domain.models.Inventory;
import application.domain.models.InventoryMovement;
import application.domain.models.Order;
import application.domain.models.Shipment;
import application.domain.models.User;
import application.domain.ports.in.BusinessOperatorPort;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/logistics")
@Validated
public class LogisticsController {

    private final BusinessOperatorPort operatorPort;
    private final OrderRestMapper orderRestMapper;
    private final ShipmentRestMapper shipmentRestMapper;
    private final InventoryMovementRestMapper inventoryMovementRestMapper;
    private final InventoryRestMapper inventoryRestMapper;
    private final ReturnRestMapper returnRestMapper;

    public LogisticsController(BusinessOperatorPort operatorPort,
                               OrderRestMapper orderRestMapper,
                               ShipmentRestMapper shipmentRestMapper,
                               InventoryMovementRestMapper inventoryMovementRestMapper,
                               InventoryRestMapper inventoryRestMapper,
                               ReturnRestMapper returnRestMapper) {
        this.operatorPort = operatorPort;
        this.orderRestMapper = orderRestMapper;
        this.shipmentRestMapper = shipmentRestMapper;
        this.inventoryMovementRestMapper = inventoryMovementRestMapper;
        this.inventoryRestMapper = inventoryRestMapper;
        this.returnRestMapper = returnRestMapper;
    }

    @GetMapping("/orders")
    public List<OrderResponse> listOrders(@RequestParam(name = "status", required = false) String status,
                                          @AuthenticationPrincipal User actor) {
        OrderStatus orderStatus = (status == null || status.isBlank())
                ? null
                : OrderStatus.fromCode(status);
        return operatorPort.listOrders(actor, orderStatus).stream()
                .map(orderRestMapper::toResponse)
                .toList();
    }

    @PostMapping("/shipments")
    public ResponseEntity<ShipmentResponse> createShipment(
            @Valid @RequestBody CreateShipmentRequest request,
            @AuthenticationPrincipal User actor) {
        Shipment shipment = operatorPort.createShipment(
                actor,
                request.orderId(),
                request.warehouseId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(shipmentRestMapper.toResponse(shipment));
    }

    @PostMapping("/shipments/{id}/start")
    public ShipmentResponse startShipment(@PathVariable String id,
                                          @Valid @RequestBody StartShipmentRequest request,
                                          @AuthenticationPrincipal User actor) {
        Shipment shipment = operatorPort.startShipping(actor, id, request.trackingNumber());
        return shipmentRestMapper.toResponse(shipment);
    }

    @PostMapping("/shipments/{id}/deliver")
    public ShipmentResponse deliverShipment(@PathVariable String id,
                                            @AuthenticationPrincipal User actor) {
        return shipmentRestMapper.toResponse(operatorPort.confirmDelivery(actor, id));
    }

    @PostMapping("/inventory/{productId}/{warehouseId}/inflow")
    public InventoryMovementResponse registerInflow(
            @PathVariable String productId,
            @PathVariable String warehouseId,
            @Valid @RequestBody InventoryAdjustmentRequest request,
            @AuthenticationPrincipal User actor) {
        if (request.quantity() == null) {
            throw new InvalidDataException("quantity is required");
        }
        InventoryMovement movement = operatorPort.registerInflow(
                actor,
                productId,
                warehouseId,
                request.quantity());
        return inventoryMovementRestMapper.toResponse(movement);
    }

    @PostMapping("/inventory/{productId}/{warehouseId}/adjust")
    public InventoryMovementResponse adjustInventory(
            @PathVariable String productId,
            @PathVariable String warehouseId,
            @Valid @RequestBody InventoryAdjustmentRequest request,
            @AuthenticationPrincipal User actor) {
        if (request.delta() == null) {
            throw new InvalidDataException("delta is required");
        }
        InventoryMovement movement = operatorPort.adjustInventory(
                actor,
                productId,
                warehouseId,
                request.delta());
        return inventoryMovementRestMapper.toResponse(movement);
    }

    @PostMapping("/inventory/{productId}/{warehouseId}/damaged")
    public InventoryResponse markDamaged(@PathVariable String productId,
                                         @PathVariable String warehouseId,
                                         @AuthenticationPrincipal User actor) {
        Inventory inventory = operatorPort.markDamaged(actor, productId, warehouseId);
        return inventoryRestMapper.toResponse(inventory);
    }

    @PostMapping("/inventory/{productId}/{warehouseId}/repaired")
    public InventoryResponse markRepaired(@PathVariable String productId,
                                          @PathVariable String warehouseId,
                                          @AuthenticationPrincipal User actor) {
        Inventory inventory = operatorPort.markRepaired(actor, productId, warehouseId);
        return inventoryRestMapper.toResponse(inventory);
    }

    @GetMapping("/returns")
    public List<ReturnResponse> listReturns(@AuthenticationPrincipal User actor) {
        return operatorPort.listReturns(actor).stream()
                .map(returnRestMapper::toResponse)
                .toList();
    }

    @PostMapping("/returns/{id}/receive")
    public ReturnResponse receiveReturn(@PathVariable String id,
                                        @AuthenticationPrincipal User actor) {
        return returnRestMapper.toResponse(operatorPort.receiveReturn(actor, id));
    }
}
