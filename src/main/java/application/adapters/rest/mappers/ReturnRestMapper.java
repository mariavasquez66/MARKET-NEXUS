package application.adapters.rest.mappers;

import application.adapters.rest.dto.response.ReturnResponse;
import application.domain.models.Return;
import org.springframework.stereotype.Component;

@Component
public class ReturnRestMapper extends RestMapperSupport {

    public ReturnResponse toResponse(Return returnRequest) {
        return new ReturnResponse(
                returnRequest.getId(),
                returnRequest.getOrder().getId(),
                returnRequest.getReason(),
                returnRequest.getStatus().getCode(),
                returnRequest.getRequestedAt());
    }
}
