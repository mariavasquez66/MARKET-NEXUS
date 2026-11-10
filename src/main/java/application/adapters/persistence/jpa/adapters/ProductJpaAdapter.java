package application.adapters.persistence.jpa.adapters;

import application.adapters.persistence.jpa.entities.ProductJpaEntity;
import application.adapters.persistence.jpa.entities.ProductVariantJpaEntity;
import application.adapters.persistence.jpa.mappers.ProductJpaMapper;
import application.adapters.persistence.jpa.repositories.ProductJpaRepository;
import application.adapters.persistence.jpa.repositories.ProductVariantJpaRepository;
import application.domain.enums.ProductStatus;
import application.domain.models.Product;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.valueobjects.ProductVariant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProductJpaAdapter implements ProductRepositoryPort {

    private final ProductJpaRepository repository;
    private final ProductVariantJpaRepository variantRepository;
    private final ProductJpaMapper mapper;
    private final JpaDependencyResolver resolver;

    @Override
    @Transactional
    public Product save(Product product) {
        ProductJpaEntity saved = repository.save(mapper.toEntity(product));
        variantRepository.deleteAll(variantRepository.findByProductId(saved.getId()));
        List<ProductVariant> variants = product.getVariants();
        List<ProductVariantJpaEntity> variantEntities = variants.stream()
                .map(variant -> mapper.toEntity(variant, saved.getId()))
                .toList();
        variantRepository.saveAll(variantEntities);
        return mapper.toDomain(saved, product.getSeller(), variants);
    }

    @Override
    public Optional<Product> findById(String id) {
        return repository.findById(id).map(resolver::productWithWarehouses);
    }

    @Override
    public List<Product> findAll() {
        return repository.findAll().stream().map(resolver::product).toList();
    }

    @Override
    public List<Product> findPublished() {
        return repository.findByStatusCode(ProductStatus.PUBLISHED).stream()
                .map(resolver::product)
                .toList();
    }

    @Override
    public List<Product> findBySellerId(String sellerId) {
        return repository.findBySellerId(sellerId).stream().map(resolver::product).toList();
    }
}
