package application.domain.models.reports;

import application.domain.enums.UserRole;
import application.domain.exceptions.common.InvalidDataException;
import java.util.Map;
import java.util.Objects;

public record UserSummary(int totalUsers, Map<UserRole, Integer> usersByRole) {
    public UserSummary {
        if (totalUsers < 0) {
            throw new InvalidDataException("Total users cannot be negative");
        }
        Objects.requireNonNull(usersByRole, "usersByRole cannot be null");
        for (Map.Entry<UserRole, Integer> entry : usersByRole.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "user role cannot be null");
            if (entry.getValue() == null || entry.getValue() < 0) {
                throw new InvalidDataException("Role counts cannot be negative");
            }
        }
        usersByRole = Map.copyOf(usersByRole);
    }
}
