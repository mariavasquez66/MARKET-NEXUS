package application.domain.ports.out;

import java.math.BigDecimal;

public interface TaxPolicyPort {
    BigDecimal currentTaxRate();
}
