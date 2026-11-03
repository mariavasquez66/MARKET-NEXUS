package application.domain.models.reports;

import application.domain.exceptions.common.InvalidDataException;

public record InventorySummary(int totalRecords,
                               int totalAvailable,
                               int totalReserved,
                               int damagedRecords) {
    public InventorySummary {
        if (totalRecords < 0 || totalAvailable < 0 || totalReserved < 0 || damagedRecords < 0) {
            throw new InvalidDataException("Inventory summary counts cannot be negative");
        }
    }
}
