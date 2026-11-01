package application.domain.ports.out;

import java.time.Instant;
import java.time.LocalDate;

public interface ClockPort {
    Instant now();

    LocalDate today();
}
