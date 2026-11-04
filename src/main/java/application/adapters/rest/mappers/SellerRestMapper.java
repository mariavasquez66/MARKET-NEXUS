package application.adapters.rest.mappers;

import application.adapters.rest.dto.response.SellerResponse;
import application.domain.models.Seller;
import application.domain.models.User;
import application.domain.models.Warehouse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SellerRestMapper extends RestMapperSupport {

    public SellerResponse toResponse(Seller seller) {
        User user = seller.getUser();
        List<String> warehouseIds = seller.getWarehouses().stream()
                .map(Warehouse::getId)
                .toList();
        return new SellerResponse(
                seller.getId(),
                user.getId(),
                user.getFullName().getValue(),
                user.getEmail().getValue(),
                seller.getTaxId().getValue(),
                List.copyOf(warehouseIds));
    }
}
