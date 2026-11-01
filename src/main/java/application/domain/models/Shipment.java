package application.domain.models;

import application.domain.enums.ShipmentStatus;
import application.domain.exceptions.common.InvalidDataException;
import application.domain.exceptions.logistics.InvalidDestinationAddressException;
import application.domain.exceptions.logistics.ShipmentAlreadyStartedException;
import application.domain.exceptions.order.InvalidStateTransitionException;
import application.domain.exceptions.product.InvalidProductTypeException;
import application.domain.valueobjects.Address;
import java.util.Objects;
import java.util.UUID;

public class Shipment {
    private final String id;
    private final Order order;
    private final Warehouse originWarehouse;
    private final Address destination;
    private String trackingNumber;
    private ShipmentStatus status;

    private Shipment(String id,
                     Order order,
                     Warehouse originWarehouse,
                     Address destination,
                     String trackingNumber,
                     ShipmentStatus status) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.order = Objects.requireNonNull(order, "order cannot be null");
        this.originWarehouse = Objects.requireNonNull(originWarehouse, "originWarehouse cannot be null");
        this.destination = Objects.requireNonNull(destination, "destination cannot be null");
        this.trackingNumber = trackingNumber;
        this.status = Objects.requireNonNull(status, "status cannot be null");
    }

    public static Shipment create(Order order, Warehouse originWarehouse) {
        Objects.requireNonNull(order, "order cannot be null");
        if (!order.requiresShipment()) {
            throw new InvalidProductTypeException("Digital orders do not require shipment");
        }
        if (order.getDeliveryAddress() == null) {
            throw new InvalidDestinationAddressException("Order delivery address is required");
        }
        return new Shipment(UUID.randomUUID().toString(), order, originWarehouse,
                order.getDeliveryAddress(), null, ShipmentStatus.PENDING);
    }

    public static Shipment reconstitute(String id,
                                        Order order,
                                        Warehouse originWarehouse,
                                        Address destination,
                                        String trackingNumber,
                                        ShipmentStatus status) {
        return new Shipment(id, order, originWarehouse, destination, trackingNumber, status);
    }

    public void startShipping(String trackingNumber) {
        if (!status.equals(ShipmentStatus.PENDING)) {
            throw new ShipmentAlreadyStartedException("Shipment has already started");
        }
        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new InvalidDataException("Tracking number is required");
        }
        this.trackingNumber = trackingNumber.trim();
        status = ShipmentStatus.IN_TRANSIT;
    }

    public void confirmDelivery() {
        if (!status.equals(ShipmentStatus.IN_TRANSIT)) {
            throw new InvalidStateTransitionException("Only in-transit shipments can be delivered");
        }
        status = ShipmentStatus.DELIVERED;
    }

    public String getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public Warehouse getOriginWarehouse() {
        return originWarehouse;
    }

    public Address getDestination() {
        return destination;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public ShipmentStatus getStatus() {
        return status;
    }
}
