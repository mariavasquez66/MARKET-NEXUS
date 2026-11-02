package application.adapters.persistence.jpa.adapters;

import application.adapters.persistence.jpa.entities.ShipmentJpaEntity;
import application.adapters.persistence.jpa.mappers.ShipmentJpaMapper;
import application.adapters.persistence.jpa.repositories.ShipmentJpaRepository;
import application.domain.models.Shipment;
import application.domain.ports.out.ShipmentRepositoryPort;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ShipmentJpaAdapter implements ShipmentRepositoryPort {

    private final ShipmentJpaRepository repository;
    private final ShipmentJpaMapper mapper;
    private final JpaDependencyResolver resolver;

    @Override
    @Transactional
    public Shipment save(Shipment shipment) {
        ShipmentJpaEntity saved = repository.save(mapper.toEntity(shipment));
        return mapper.toDomain(saved, shipment.getOrder(), shipment.getOriginWarehouse());
    }

    @Override
    public Optional<Shipment> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Shipment> findByOrderId(String orderId) {
        return repository.findByOrderId(orderId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Shipment> findByOrderIdAndOriginWarehouseId(String orderId, String warehouseId) {
        return repository.findByOrderIdAndOriginWarehouseId(orderId, warehouseId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Shipment> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    private Shipment toDomain(ShipmentJpaEntity entity) {
        return mapper.toDomain(
                entity,
                resolver.orderWithoutInvoice(entity.getOrderId()),
                resolver.warehouse(entity.getOriginWarehouseId()));
    }
}
