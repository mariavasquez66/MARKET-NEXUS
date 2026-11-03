package application.adapters.rest.dto.response;

import java.util.Map;

public record ReturnSummaryResponse(
        int totalReturns,
        Map<String, Integer> returnsByStatus,
        int approved,
        int rejected,
        int completed) {
}
