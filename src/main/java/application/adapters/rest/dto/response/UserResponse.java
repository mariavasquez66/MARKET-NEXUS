package application.adapters.rest.dto.response;

public record UserResponse(
        String id,
        String fullName,
        String email,
        String identityDocument,
        String role,
        String status,
        long authTokenVersion) {
}
