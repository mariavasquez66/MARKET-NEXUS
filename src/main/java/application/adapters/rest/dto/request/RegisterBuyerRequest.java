package application.adapters.rest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterBuyerRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 5, max = 30) String identityDocument,
        @NotBlank @Size(min = 5) String password,
        @NotNull @Valid AddressRequest mainAddress) {
}
