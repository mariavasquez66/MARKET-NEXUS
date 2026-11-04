package application.adapters.rest.mappers;

import application.adapters.rest.dto.response.RefundResponse;
import application.domain.models.Refund;
import org.springframework.stereotype.Component;

@Component
public class RefundRestMapper extends RestMapperSupport {

    public RefundResponse toResponse(Refund refund) {
        return new RefundResponse(
                refund.getId(),
                refund.getReturnRequest().getId(),
                toMoneyResponse(refund.getAmount()),
                refund.getStatus().getCode());
    }
}
