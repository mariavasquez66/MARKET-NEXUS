package application.domain.ports.out;

import application.domain.models.Shipment;
import java.util.List;
import java.util.Optional;

public interface ShipmentRepositoryPort {
    Shipment save(Shipment shipment);

    Optional<Shipment> findById(String id);

    List<Shipment> findByOrderId(String orderId);

    List<Shipment> findByOrderIdAndOriginWarehouseId(String orderId, String warehouseId);

    List<Shipment> findAll();
}
