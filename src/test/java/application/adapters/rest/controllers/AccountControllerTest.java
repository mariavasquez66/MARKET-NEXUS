package application.adapters.rest.controllers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import application.domain.DomainFixtures;
import application.domain.enums.UserRole;
import application.domain.models.User;
import application.domain.ports.in.AccountPort;
import application.infrastructure.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccountPort accountPort;

    private MockMvc mockMvc;
    private User actor;

    @BeforeEach
    void setUp() {
        actor = DomainFixtures.user(UserRole.BUYER);
        mockMvc = MockMvcBuilders.standaloneSetup(new AccountController(accountPort))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(RestControllerTestSupport.actorResolver())
                .build();
    }

    @Test
    void logoutInvalidatesTheCurrentSession() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(RestControllerTestSupport.actor(actor)))
                .andExpect(status().isNoContent());

        verify(accountPort).logout(actor);
    }

    @Test
    void changePasswordRequiresAValidPolicy() throws Exception {
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"current123","newPassword":"weak"}
                                """)
                        .with(RestControllerTestSupport.actor(actor)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(accountPort);
    }

    @Test
    void changePasswordDelegatesToUseCase() throws Exception {
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"current123","newPassword":"newpass123"}
                                """)
                        .with(RestControllerTestSupport.actor(actor)))
                .andExpect(status().isNoContent());

        verify(accountPort).changePassword(actor, "current123", "newpass123");
    }
}
