package application.infrastructure.security;

import application.domain.models.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecurityProbeController {

    @GetMapping("/api/v1/public/ping")
    public String publicPing() {
        return "ok";
    }

    @GetMapping("/api/v1/buyer/ping")
    public String buyerPing(@AuthenticationPrincipal(expression = "user") User user) {
        return user.getId();
    }

    @GetMapping("/api/v1/seller/ping")
    public String sellerPing(@AuthenticationPrincipal(expression = "user") User user) {
        return user.getId();
    }

    @GetMapping("/api/v1/logistics/ping")
    public String logisticsPing(@AuthenticationPrincipal(expression = "user") User user) {
        return user.getId();
    }

    @GetMapping("/api/v1/admin/ping")
    public String adminPing(@AuthenticationPrincipal(expression = "user") User user) {
        return user.getId();
    }

    @GetMapping("/api/v1/supervisor/ping")
    public String supervisorPing(@AuthenticationPrincipal(expression = "user") User user) {
        return user.getId();
    }

    @PostMapping("/api/v1/supervisor/ping")
    public String supervisorPost() {
        return "unexpected";
    }
}
