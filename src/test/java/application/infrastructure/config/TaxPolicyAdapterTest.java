package application.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TaxPolicyAdapterTest {

    @Test
    void exposesConfiguredTaxRate() {
        assertThat(new TaxPolicyAdapter("0.19").currentTaxRate())
                .isEqualByComparingTo(new BigDecimal("0.19"));
    }

    @Test
    void rejectsRatesOutsideTheValidRange() {
        assertThatThrownBy(() -> new TaxPolicyAdapter("-0.01"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new TaxPolicyAdapter("1.01"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new TaxPolicyAdapter("not-a-number"))
                .isInstanceOf(NumberFormatException.class);
    }
}
