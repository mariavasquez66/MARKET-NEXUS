package application.adapters.persistence.jpa.adapters;

import application.adapters.persistence.jpa.entities.InvoiceJpaEntity;
import application.adapters.persistence.jpa.mappers.InvoiceJpaMapper;
import application.adapters.persistence.jpa.repositories.InvoiceJpaRepository;
import application.domain.models.Invoice;
import application.domain.ports.out.InvoiceRepositoryPort;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class InvoiceJpaAdapter implements InvoiceRepositoryPort {

    private final InvoiceJpaRepository repository;
    private final InvoiceJpaMapper mapper;
    private final JpaDependencyResolver resolver;

    @Override
    @Transactional
    public Invoice save(Invoice invoice) {
        InvoiceJpaEntity saved = repository.save(mapper.toEntity(invoice));
        return mapper.toDomain(saved, invoice.getOrder());
    }

    @Override
    public Optional<Invoice> findById(String id) {
        return repository.findById(id).map(resolver::invoice);
    }

    @Override
    public Optional<Invoice> findByOrderId(String orderId) {
        return repository.findByOrderId(orderId).map(resolver::invoice);
    }

    @Override
    public List<Invoice> findByBuyerId(String buyerId) {
        return repository.findByBuyerId(buyerId).stream().map(resolver::invoice).toList();
    }

    @Override
    public boolean existsByOrderId(String orderId) {
        return repository.existsByOrderId(orderId);
    }
}
