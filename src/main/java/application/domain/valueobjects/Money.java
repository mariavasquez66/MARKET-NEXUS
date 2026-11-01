package application.domain.valueobjects;

import application.domain.exceptions.common.InvalidDataException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public final class Money {
    public static final Currency DEFAULT_CURRENCY = Currency.getInstance("COP");
    private static final int SCALE = 2;

    private final BigDecimal amount;
    private final Currency currency;

    private Money(BigDecimal amount, Currency currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public static Money of(BigDecimal amount, Currency currency) {
        if (amount == null) {
            throw new InvalidDataException("Money amount cannot be null");
        }
        if (currency == null) {
            throw new InvalidDataException("Money currency cannot be null");
        }
        BigDecimal normalized = amount.setScale(SCALE, RoundingMode.HALF_UP);
        if (normalized.signum() < 0) {
            throw new InvalidDataException("Money amount cannot be negative");
        }
        return new Money(normalized, currency);
    }

    public static Money of(BigDecimal amount) {
        return of(amount, DEFAULT_CURRENCY);
    }

    public static Money cop(BigDecimal amount) {
        return of(amount, DEFAULT_CURRENCY);
    }

    public Money add(Money other) {
        assertSameCurrency(other);
        return of(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        assertSameCurrency(other);
        BigDecimal result = amount.subtract(other.amount);
        if (result.signum() < 0) {
            throw new InvalidDataException("Money subtraction cannot produce a negative amount");
        }
        return of(result, currency);
    }

    public Money multiply(int factor) {
        if (factor < 0) {
            throw new InvalidDataException("Money multiplier cannot be negative");
        }
        return of(amount.multiply(BigDecimal.valueOf(factor)), currency);
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isGreaterThan(Money other) {
        assertSameCurrency(other);
        return amount.compareTo(other.amount) > 0;
    }

    public boolean isLessThanOrEqualTo(Money other) {
        assertSameCurrency(other);
        return amount.compareTo(other.amount) <= 0;
    }

    private void assertSameCurrency(Money other) {
        if (other == null) {
            throw new InvalidDataException("Money operand cannot be null");
        }
        if (!currency.equals(other.currency)) {
            throw new InvalidDataException("Money currencies do not match");
        }
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money money)) {
            return false;
        }
        return amount.compareTo(money.amount) == 0 && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.getCurrencyCode();
    }
}
