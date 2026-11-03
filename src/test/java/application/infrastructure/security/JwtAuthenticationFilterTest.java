package application.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import application.domain.enums.UserRole;
import application.domain.enums.UserStatus;
import application.domain.models.User;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.valueobjects.Email;
import application.domain.valueobjects.FullName;
import application.domain.valueobjects.IdentityDocument;
import application.domain.valueobjects.PasswordHash;
import jakarta.servlet.FilterChain;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    private static final String SECRET = "test-secret-for-jwt-filter-with-enough-bytes-0123456789";

    @Mock
    private UserRepositoryPort userRepository;

    private MutableClock clock;
    private JwtProvider jwtProvider;
    private JwtAuthenticationFilter filter;
    private User user;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
        jwtProvider = new JwtProvider(SECRET, 3_600_000L, clock);
        filter = new JwtAuthenticationFilter(jwtProvider, userRepository,
                new SecurityErrorWriter(clock));
        user = User.reconstitute(
                "user-1",
                FullName.of("Jane Buyer"),
                Email.of("jane@nexusmarket.test"),
                IdentityDocument.of("DOC12345"),
                UserRole.BUYER,
                UserStatus.ACTIVE,
                PasswordHash.of("$2a$10$stored-hash"),
                0L);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void letsPublicAndPreflightRequestsContinueWithoutToken() throws Exception {
        MockHttpServletRequest publicRequest = request("GET", "/api/v1/public/products");
        MockHttpServletResponse publicResponse = new MockHttpServletResponse();
        AtomicBoolean publicChainCalled = new AtomicBoolean(false);

        filter.doFilter(publicRequest, publicResponse, chain(publicChainCalled));

        assertThat(publicChainCalled).isTrue();
        assertThat(publicResponse.getStatus()).isEqualTo(200);

        MockHttpServletRequest preflight = request("OPTIONS", "/api/v1/buyer/cart");
        MockHttpServletResponse preflightResponse = new MockHttpServletResponse();
        AtomicBoolean preflightChainCalled = new AtomicBoolean(false);

        filter.doFilter(preflight, preflightResponse, chain(preflightChainCalled));

        assertThat(preflightChainCalled).isTrue();
        assertThat(preflightResponse.getStatus()).isEqualTo(200);
    }

    @Test
    void leavesMissingTokenToTheAuthenticationEntryPoint() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/v1/buyer/cart");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.doFilter(request, response, chain(chainCalled));

        assertThat(chainCalled).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void authenticatesValidTokenWithCurrentDomainUser() throws Exception {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        MockHttpServletRequest request = request("GET", "/api/v1/buyer/cart");
        request.addHeader("Authorization", "Bearer " + jwtProvider.issueToken(user));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.doFilter(request, response, chain(chainCalled));

        assertThat(chainCalled).isTrue();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_BUYER");
        assertThat(authentication.getPrincipal())
                .isInstanceOf(AuthenticatedUserPrincipal.class);
        assertThat(((AuthenticatedUserPrincipal) authentication.getPrincipal()).getUser())
                .isSameAs(user);
    }

    @Test
    void rejectsInvalidToken() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/v1/buyer/cart");
        request.addHeader("Authorization", "Bearer definitely-not-a-jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.doFilter(request, response, chain(chainCalled));

        assertUnauthorized(response, chainCalled);
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        String token = jwtProvider.issueToken(user);
        clock.advanceMillis(3_600_001L);
        MockHttpServletRequest request = request("GET", "/api/v1/buyer/cart");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.doFilter(request, response, chain(chainCalled));

        assertUnauthorized(response, chainCalled);
    }

    @Test
    void rejectsUnknownUser() throws Exception {
        when(userRepository.findById("user-1")).thenReturn(Optional.empty());
        MockHttpServletRequest request = authenticatedRequest();

        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        filter.doFilter(request, response, chain(chainCalled));

        assertUnauthorized(response, chainCalled);
    }

    @Test
    void rejectsInactiveOrBlockedUser() throws Exception {
        String token = jwtProvider.issueToken(user);
        User blocked = User.reconstitute(user.getId(), user.getFullName(), user.getEmail(),
                user.getIdentityDocument(), user.getRole(), UserStatus.BLOCKED,
                user.getPasswordHash(), user.getAuthTokenVersion());
        when(userRepository.findById("user-1")).thenReturn(Optional.of(blocked));
        MockHttpServletRequest request = request("GET", "/api/v1/buyer/cart");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.doFilter(request, response, chain(chainCalled));

        assertUnauthorized(response, chainCalled);
    }

    @Test
    void rejectsObsoleteTokenVersion() throws Exception {
        String token = jwtProvider.issueToken(user);
        user.invalidateTokens();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        MockHttpServletRequest request = request("GET", "/api/v1/buyer/cart");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.doFilter(request, response, chain(chainCalled));

        assertUnauthorized(response, chainCalled);
    }

    @Test
    void rejectsRoleClaimThatDoesNotMatchCurrentUserRole() throws Exception {
        String token = jwtProvider.issueToken(user);
        User seller = User.reconstitute(user.getId(), user.getFullName(), user.getEmail(),
                user.getIdentityDocument(), UserRole.SELLER, user.getStatus(),
                user.getPasswordHash(), user.getAuthTokenVersion());
        when(userRepository.findById("user-1")).thenReturn(Optional.of(seller));
        MockHttpServletRequest request = request("GET", "/api/v1/seller/products");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.doFilter(request, response, chain(chainCalled));

        assertUnauthorized(response, chainCalled);
    }

    private MockHttpServletRequest authenticatedRequest() {
        MockHttpServletRequest request = request("GET", "/api/v1/buyer/cart");
        request.addHeader("Authorization", "Bearer " + jwtProvider.issueToken(user));
        return request;
    }

    private void assertUnauthorized(MockHttpServletResponse response, AtomicBoolean chainCalled)
            throws Exception {
        assertThat(chainCalled).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"code\":\"INVALID_TOKEN\"");
        assertThat(response.getHeader("X-Request-Id")).isNotBlank();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private MockHttpServletRequest request(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRequestURI(uri);
        return request;
    }

    private FilterChain chain(AtomicBoolean called) {
        return (request, response) -> called.set(true);
    }

    private static final class MutableClock implements ClockPort {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        void advanceMillis(long millis) {
            now = now.plusMillis(millis);
        }

        @Override
        public Instant now() {
            return now;
        }

        @Override
        public LocalDate today() {
            return LocalDate.ofInstant(now, ZoneOffset.UTC);
        }
    }
}
