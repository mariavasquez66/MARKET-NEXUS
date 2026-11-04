package application.infrastructure.security;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import application.domain.DomainFixtures;
import application.domain.enums.UserRole;
import application.domain.models.User;
import application.domain.ports.out.UserRepositoryPort;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SecurityProbeController.class)
@Import({
        SecurityConfig.class,
        JwtProvider.class,
        SystemClockAdapter.class,
        SecurityErrorWriter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-for-security-config-with-enough-bytes-0123456789",
        "jwt.expiration-ms=3600000",
        "nexusmarket.cors.allowed-origins=http://localhost:3000"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private UserRepositoryPort userRepository;

    @Test
    void allowsPublicEndpointWithoutTokenAndReturnsRequestId() throws Exception {
        mockMvc.perform(get("/api/v1/public/ping"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", not(blankOrNullString())));
    }

    @Test
    void rejectsProtectedEndpointWithoutTokenWithJsonErrorAndRequestId() throws Exception {
        String requestId = "test-request-id";
        mockMvc.perform(get("/api/v1/buyer/ping").header("X-Request-Id", requestId))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Request-Id", requestId))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.path").value("/api/v1/buyer/ping"))
                .andExpect(jsonPath("$.requestId").value(requestId));
    }

    @Test
    void rejectsInvalidTokenWithInvalidTokenCode() throws Exception {
        mockMvc.perform(get("/api/v1/buyer/ping")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @ParameterizedTest
    @MethodSource("allowedRoleRequests")
    void allowsExpectedRoleForEachProtectedPrefix(String path, UserRole role) throws Exception {
        User user = DomainFixtures.user(role);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        mockMvc.perform(get(path)
                        .header("Authorization", "Bearer " + jwtProvider.issueToken(user)))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @MethodSource("forbiddenRoleRequests")
    void rejectsWrongRoleForEachProtectedPrefix(String path, UserRole role) throws Exception {
        User user = DomainFixtures.user(role);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        mockMvc.perform(get(path)
                        .header("Authorization", "Bearer " + jwtProvider.issueToken(user)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void rejectsNonGetRequestForSupervisor() throws Exception {
        User supervisor = DomainFixtures.user(UserRole.SUPERVISOR);
        when(userRepository.findById(supervisor.getId())).thenReturn(Optional.of(supervisor));

        mockMvc.perform(post("/api/v1/supervisor/ping")
                        .header("Authorization", "Bearer " + jwtProvider.issueToken(supervisor)))
                .andExpect(status().isForbidden());
    }

    @Test
    void answersCorsPreflightWithoutToken() throws Exception {
        mockMvc.perform(options("/api/v1/buyer/ping")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type, X-Request-Id"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Headers",
                        "Authorization, Content-Type, X-Request-Id"));
    }

    private static Stream<Arguments> allowedRoleRequests() {
        return Stream.of(
                Arguments.of("/api/v1/buyer/ping", UserRole.BUYER),
                Arguments.of("/api/v1/seller/ping", UserRole.SELLER),
                Arguments.of("/api/v1/logistics/ping", UserRole.LOGISTIC_OPERATOR),
                Arguments.of("/api/v1/admin/ping", UserRole.ADMIN),
                Arguments.of("/api/v1/supervisor/ping", UserRole.SUPERVISOR));
    }

    private static Stream<Arguments> forbiddenRoleRequests() {
        return Stream.of(
                Arguments.of("/api/v1/buyer/ping", UserRole.SELLER),
                Arguments.of("/api/v1/seller/ping", UserRole.BUYER),
                Arguments.of("/api/v1/logistics/ping", UserRole.BUYER),
                Arguments.of("/api/v1/admin/ping", UserRole.BUYER),
                Arguments.of("/api/v1/supervisor/ping", UserRole.BUYER));
    }
}
