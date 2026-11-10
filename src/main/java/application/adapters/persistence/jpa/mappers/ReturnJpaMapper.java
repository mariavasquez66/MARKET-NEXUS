package application.adapters.persistence.jpa.mappers;

import application.adapters.persistence.jpa.entities.ReturnJpaEntity;
import application.domain.models.Order;
import application.domain.models.Return;
import org.springframework.stereotype.Component;

@Component
public class ReturnJpaMapper {

    public ReturnJpaEntity toEntity(Return returnRequest) {
        ReturnJpaEntity entity = new ReturnJpaEntity();
        entity.setId(returnRequest.getId());
        entity.setOrderId(returnRequest.getOrder().getId());
        entity.setProductId(returnRequest.getProductId());
        entity.setQuantity(returnRequest.getQuantity());
        entity.setReason(returnRequest.getReason());
        entity.setStatusCode(returnRequest.getStatus());
        entity.setRequestedAt(returnRequest.getRequestedAt());
        return entity;
    }

    public Return toDomain(ReturnJpaEntity entity, Order order) {
        return Return.reconstitute(
                entity.getId(),
                order,
                entity.getReason(),
                entity.getProductId(),
                entity.getQuantity(),
                entity.getStatusCode(),
                entity.getRequestedAt());
    }
}
