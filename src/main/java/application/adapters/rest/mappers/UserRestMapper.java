package application.adapters.rest.mappers;

import application.adapters.rest.dto.response.AuthResponse;
import application.adapters.rest.dto.response.UserResponse;
import application.domain.models.AuthResult;
import application.domain.models.User;
import org.springframework.stereotype.Component;

@Component
public class UserRestMapper extends RestMapperSupport {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName().getValue(),
                user.getEmail().getValue(),
                user.getIdentityDocument().getValue(),
                user.getRole().getCode(),
                user.getStatus().getCode(),
                user.getAuthTokenVersion());
    }

    public AuthResponse toResponse(AuthResult result) {
        return new AuthResponse(result.token(), toResponse(result.user()));
    }
}
