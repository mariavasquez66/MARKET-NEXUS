package application.adapters.rest.dto.response;

public record AuthResponse(String token, UserResponse user) {
}
