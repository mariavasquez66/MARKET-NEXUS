package application.adapters.persistence.jpa.mappers;

import application.adapters.persistence.jpa.entities.RefundJpaEntity;
import application.domain.models.Refund;
import application.domain.models.Return;
import application.domain.valueobjects.Money;
import java.util.Currency;
import org.springframework.stereotype.Component;

@Component
public class RefundJpaMapper {

    public RefundJpaEntity toEntity(Refund refund) {
        RefundJpaEntity entity = new RefundJpaEntity();
        entity.setId(refund.getId());
        entity.setReturnId(refund.getReturnRequest().getId());
        entity.setAmountAmount(refund.getAmount().getAmount());
        entity.setAmountCurrency(refund.getAmount().getCurrency().getCurrencyCode());
        entity.setStatusCode(refund.getStatus());
        return entity;
    }

    public Refund toDomain(RefundJpaEntity entity, Return returnRequest) {
        return Refund.reconstitute(
                entity.getId(),
                returnRequest,
                Money.of(entity.getAmountAmount(), Currency.getInstance(entity.getAmountCurrency())),
                entity.getStatusCode());
    }
}
