package application.infrastructure.security;

import application.domain.enums.UserStatus;
import application.domain.models.User;
import application.domain.ports.out.UserRepositoryPort;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String PUBLIC_PREFIX = "/api/v1/public/";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final UserRepositoryPort userRepository;
    private final SecurityErrorWriter errorWriter;

    public JwtAuthenticationFilter(JwtProvider jwtProvider,
                                   UserRepositoryPort userRepository,
                                   SecurityErrorWriter errorWriter) {
        this.jwtProvider = Objects.requireNonNull(jwtProvider, "jwtProvider cannot be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository cannot be null");
        this.errorWriter = Objects.requireNonNull(errorWriter, "errorWriter cannot be null");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isPublicRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        JwtTokenClaims claims;
        try {
            claims = jwtProvider.validateToken(authorization.substring(BEARER_PREFIX.length()).trim());
        } catch (JwtException ex) {
            unauthorized(response, request);
            return;
        }

        Optional<User> found = userRepository.findById(claims.subject());
        if (found.isEmpty()) {
            unauthorized(response, request);
            return;
        }
        User user = found.get();
        if (user.getStatus() != UserStatus.ACTIVE
                || user.getAuthTokenVersion() != claims.tokenVersion()
                || !user.getRole().equals(claims.role())) {
            unauthorized(response, request);
            return;
        }

        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(user);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().getCode())));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        filterChain.doFilter(request, response);
    }

    private boolean isPublicRequest(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return request.getRequestURI().startsWith(PUBLIC_PREFIX);
    }

    private void unauthorized(HttpServletResponse response, HttpServletRequest request)
            throws IOException {
        SecurityContextHolder.clearContext();
        errorWriter.write(request, response, HttpStatus.UNAUTHORIZED.value(),
                "INVALID_TOKEN", "Invalid or expired token");
    }
}
