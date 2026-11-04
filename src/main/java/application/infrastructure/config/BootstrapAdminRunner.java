package application.infrastructure.config;

import application.domain.enums.UserRole;
import application.domain.models.User;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.services.UserService;
import application.domain.valueobjects.Email;
import application.domain.valueobjects.FullName;
import application.domain.valueobjects.IdentityDocument;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class BootstrapAdminRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);
    private static final String BOOTSTRAP_ADMIN_NAME = "Bootstrap Administrator";
    private static final String BOOTSTRAP_ADMIN_DOCUMENT = "BOOTSTRAP-ADMIN";

    private final UserRepositoryPort userRepository;
    private final UserService userService;
    private final String adminEmail;
    private final String adminPassword;

    public BootstrapAdminRunner(
            UserRepositoryPort userRepository,
            UserService userService,
            @Value("${NEXUS_BOOTSTRAP_ADMIN_EMAIL:}") String adminEmail,
            @Value("${NEXUS_BOOTSTRAP_ADMIN_PASSWORD:}") String adminPassword) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository cannot be null");
        this.userService = Objects.requireNonNull(userService, "userService cannot be null");
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            return;
        }
        if (isBlank(adminEmail) || isBlank(adminPassword)) {
            log.warn("No administrator exists and NEXUS_BOOTSTRAP_ADMIN_EMAIL or "
                    + "NEXUS_BOOTSTRAP_ADMIN_PASSWORD is missing; administrator bootstrap skipped");
            return;
        }
        User admin = userService.createBootstrapAdmin(
                FullName.of(BOOTSTRAP_ADMIN_NAME),
                Email.of(adminEmail),
                IdentityDocument.of(BOOTSTRAP_ADMIN_DOCUMENT),
                adminPassword);
        log.info("Bootstrap administrator created with id {}", admin.getId());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
