package application.adapters.rest.dto.response;

import java.util.Map;

public record UserSummaryResponse(
        int totalUsers,
        Map<String, Integer> usersByRole,
        int active,
        int blocked,
        int inactive) {
}
