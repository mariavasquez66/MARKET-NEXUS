package application.adapters.rest.dto.response;

import java.util.Map;

public record InventorySummaryResponse(
        int totalRecords,
        int totalAvailable,
        int totalReserved,
        int damagedRecords,
        Map<String, Integer> byWarehouse) {
}
