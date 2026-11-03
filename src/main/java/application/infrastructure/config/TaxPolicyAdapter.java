package application.infrastructure.config;

import application.domain.ports.out.TaxPolicyPort;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TaxPolicyAdapter implements TaxPolicyPort {
    private final BigDecimal taxRate;

    public TaxPolicyAdapter(@Value("${nexusmarket.tax.rate:0.19}") String taxRate) {
        BigDecimal parsed = new BigDecimal(taxRate);
        if (parsed.compareTo(BigDecimal.ZERO) < 0 || parsed.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalStateException("nexusmarket.tax.rate must be between 0 and 1");
        }
        this.taxRate = parsed;
    }

    @Override
    public BigDecimal currentTaxRate() {
        return taxRate;
    }
}
