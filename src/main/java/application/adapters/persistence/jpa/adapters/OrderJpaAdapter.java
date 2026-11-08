package application.adapters.persistence.jpa.adapters;

import application.adapters.persistence.jpa.entities.OrderAllocationJpaEntity;
import application.adapters.persistence.jpa.entities.OrderItemJpaEntity;
import application.adapters.persistence.jpa.entities.OrderJpaEntity;
import application.adapters.persistence.jpa.mappers.OrderJpaMapper;
import application.adapters.persistence.jpa.repositories.OrderAllocationJpaRepository;
import application.adapters.persistence.jpa.repositories.OrderItemJpaRepository;
import application.adapters.persistence.jpa.repositories.OrderJpaRepository;
import application.domain.enums.OrderStatus;
import application.domain.enums.PaymentStatus;
import application.domain.models.Order;
import application.domain.ports.out.OrderRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrderJpaAdapter implements OrderRepositoryPort {

    private final OrderJpaRepository repository;
    private final OrderItemJpaRepository itemRepository;
    private final OrderAllocationJpaRepository allocationRepository;
    private final OrderJpaMapper mapper;
    private final JpaDependencyResolver resolver;

    @Override
    @Transactional
    public Order save(Order order) {
        OrderJpaEntity entity = repository.findById(order.getId())
                .map(existing -> mapper.updateEntity(existing, order))
                .orElseGet(() -> mapper.toEntity(order));
        OrderJpaEntity saved = repository.save(entity);
        itemRepository.deleteAll(itemRepository.findByOrderId(saved.getId()));
        allocationRepository.deleteAll(allocationRepository.findByOrderId(saved.getId()));
        List<OrderItemJpaEntity> itemEntities = order.getItems().stream()
                .map(item -> mapper.toEntity(item, saved.getId()))
                .toList();
        List<OrderAllocationJpaEntity> allocationEntities = order.getAllocations().stream()
                .map(allocation -> mapper.toEntity(allocation, saved.getId()))
                .toList();
        itemRepository.saveAll(itemEntities);
        allocationRepository.saveAll(allocationEntities);
        return mapper.toDomain(
                saved,
                order.getBuyer(),
                order.getItems(),
                order.getAllocations(),
                order.getInvoice());
    }

    @Override
    public Optional<Order> findById(String id) {
        return repository.findById(id).map(resolver::order);
    }

    @Override
    public List<Order> findAll() {
        return resolver.orders(repository.findAll());
    }

    @Override
    public List<Order> findByBuyerId(String buyerId) {
        return resolver.orders(repository.findByBuyerId(buyerId));
    }

    @Override
    public List<Order> findBySellerId(String sellerId) {
        return resolver.orders(repository.findDistinctBySellerId(sellerId));
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        return resolver.orders(repository.findByStatusCode(status));
    }

    @Override
    public List<Order> findByBuyerIdAndStatus(String buyerId, OrderStatus status) {
        return resolver.orders(repository.findByBuyerIdAndStatusCode(buyerId, status));
    }

    @Override
    public List<Order> findByPaymentStatusAndCreatedAtBetween(
            PaymentStatus paymentStatus, Instant from, Instant to) {
        return resolver.orders(repository.findByPaymentStatusCodeAndCreatedAtBetween(
                paymentStatus, from, to));
    }
}
