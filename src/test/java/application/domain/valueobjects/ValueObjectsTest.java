package application.domain.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import application.domain.enums.VariantType;
import application.domain.exceptions.buyer.InvalidAddressException;
import application.domain.exceptions.common.InvalidDataException;
import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class ValueObjectsTest {

    @Test
    void moneyNormalizesScaleAndSupportsArithmetic() {
        Money money = Money.cop(new BigDecimal("10.005"));
        assertThat(money.getAmount()).isEqualByComparingTo("10.01");
        assertThat(money.add(Money.cop(new BigDecimal("2.50"))).getAmount())
                .isEqualByComparingTo("12.51");
        assertThat(money.multiply(3).getAmount()).isEqualByComparingTo("30.03");
        assertThat(money.isPositive()).isTrue();
        assertThat(Money.cop(BigDecimal.ZERO).isZero()).isTrue();
    }

    @Test
    void moneyRejectsNegativeValuesAndCurrencyMismatch() {
        assertThatThrownBy(() -> Money.cop(new BigDecimal("-1")))
                .isInstanceOf(InvalidDataException.class);
        Money usd = Money.of(BigDecimal.ONE, Currency.getInstance("USD"));
        assertThatThrownBy(() -> Money.cop(BigDecimal.ONE).add(usd))
                .isInstanceOf(InvalidDataException.class);
        assertThatThrownBy(() -> Money.cop(BigDecimal.ONE).subtract(Money.cop(BigDecimal.TEN)))
                .isInstanceOf(InvalidDataException.class);
    }

    @Test
    void emailIsNormalizedAndValidated() {
        Email email = Email.of("  Test.User@Example.COM ");
        assertThat(email.getValue()).isEqualTo("test.user@example.com");
        assertThatThrownBy(() -> Email.of("not-an-email"))
                .isInstanceOf(InvalidDataException.class);
    }

    @Test
    void addressRequiresStreetCityRegionAndCountry() {
        Address address = Address.of("Calle 1", "Bogota", "Cundinamarca", " ", "Colombia");
        assertThat(address.getPostalCode()).isNull();
        assertThatThrownBy(() -> Address.of("", "Bogota", "Cundinamarca", "1", "Colombia"))
                .isInstanceOf(InvalidAddressException.class);
    }

    @Test
    void identityDocumentIsUppercasedAndValidated() {
        assertThat(IdentityDocument.of(" ab-123 ").getValue()).isEqualTo("AB-123");
        assertThatThrownBy(() -> IdentityDocument.of("ab"))
                .isInstanceOf(InvalidDataException.class);
    }

    @Test
    void taxIdValidatesFormat() {
        assertThat(TaxId.of("900123456-7").getValue()).isEqualTo("900123456-7");
        assertThatThrownBy(() -> TaxId.of("abc"))
                .isInstanceOf(InvalidDataException.class);
    }

    @Test
    void fullNameHasLengthLimits() {
        assertThatThrownBy(() -> FullName.of("A"))
                .isInstanceOf(InvalidDataException.class);
        assertThat(FullName.of("  Ana Perez  ").getValue()).isEqualTo("Ana Perez");
    }

    @Test
    void productVariantComparesIgnoringValueCase() {
        ProductVariant variant = ProductVariant.of(VariantType.COLOR, "Rojo");
        assertThat(variant).isEqualTo(ProductVariant.of(VariantType.COLOR, "rojo"));
        assertThatThrownBy(() -> ProductVariant.of(null, "Rojo"))
                .isInstanceOf(InvalidDataException.class);
    }

    @Test
    void passwordHashNeverExposesValueInToString() {
        PasswordHash hash = PasswordHash.of("secret-hash");
        assertThat(hash.toString()).isEqualTo("***");
        assertThatThrownBy(() -> PasswordHash.of(" "))
                .isInstanceOf(InvalidDataException.class);
    }

    @Test
    void stockAllocationRequiresPositiveQuantity() {
        assertThatThrownBy(() -> StockAllocation.of("p", "w", 0))
                .isInstanceOf(InvalidDataException.class);
        assertThat(StockAllocation.of("p", "w", 2).getQuantity()).isEqualTo(2);
    }
}
