package application.domain.models.reports;

import application.domain.exceptions.common.InvalidDataException;

public record ReturnSummary(int totalReturns,
                            int requested,
                            int approved,
                            int rejected,
                            int completed) {
    public ReturnSummary {
        if (totalReturns < 0 || requested < 0 || approved < 0 || rejected < 0 || completed < 0) {
            throw new InvalidDataException("Return summary counts cannot be negative");
        }
    }
}
