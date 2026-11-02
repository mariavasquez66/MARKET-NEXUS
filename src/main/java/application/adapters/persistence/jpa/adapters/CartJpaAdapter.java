package application.adapters.persistence.jpa.adapters;

import application.adapters.persistence.jpa.entities.CartItemJpaEntity;
import application.adapters.persistence.jpa.entities.CartJpaEntity;
import application.adapters.persistence.jpa.mappers.CartJpaMapper;
import application.adapters.persistence.jpa.repositories.CartItemJpaRepository;
import application.adapters.persistence.jpa.repositories.CartJpaRepository;
import application.domain.models.Cart;
import application.domain.ports.out.CartRepositoryPort;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CartJpaAdapter implements CartRepositoryPort {

    private final CartJpaRepository repository;
    private final CartItemJpaRepository itemRepository;
    private final CartJpaMapper mapper;
    private final JpaDependencyResolver resolver;

    @Override
    @Transactional
    public Cart save(Cart cart) {
        CartJpaEntity saved = repository.save(mapper.toEntity(cart));
        itemRepository.deleteByCartId(saved.getId());
        List<CartItemJpaEntity> itemEntities = cart.getItems().stream()
                .map(item -> mapper.toEntity(item, saved.getId()))
                .toList();
        itemRepository.saveAll(itemEntities);
        return mapper.toDomain(saved, cart.getBuyer(), cart.getItems());
    }

    @Override
    public Optional<Cart> findByBuyerId(String buyerId) {
        return repository.findByBuyerId(buyerId)
                .map(entity -> mapper.toDomain(
                        entity,
                        resolver.buyer(entity.getBuyerId()),
                        resolver.cartItems(entity.getId())));
    }

    @Override
    @Transactional
    public void deleteByBuyerId(String buyerId) {
        repository.findByBuyerId(buyerId)
                .ifPresent(cart -> itemRepository.deleteByCartId(cart.getId()));
        repository.deleteByBuyerId(buyerId);
    }
}
