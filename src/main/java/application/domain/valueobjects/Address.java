package application.domain.valueobjects;

import application.domain.exceptions.buyer.InvalidAddressException;
import java.util.Objects;

public final class Address {
    private final String street;
    private final String city;
    private final String region;
    private final String postalCode;
    private final String country;

    private Address(String street, String city, String region, String postalCode, String country) {
        this.street = street;
        this.city = city;
        this.region = region;
        this.postalCode = postalCode;
        this.country = country;
    }

    public static Address of(String street, String city, String region, String postalCode, String country) {
        return new Address(
                requireText(street, "street"),
                requireText(city, "city"),
                requireText(region, "region"),
                normalizeOptional(postalCode),
                requireText(country, "country"));
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidAddressException("Address " + field + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public String getStreet() {
        return street;
    }

    public String getCity() {
        return city;
    }

    public String getRegion() {
        return region;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountry() {
        return country;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Address address)) {
            return false;
        }
        return Objects.equals(street, address.street)
                && Objects.equals(city, address.city)
                && Objects.equals(region, address.region)
                && Objects.equals(postalCode, address.postalCode)
                && Objects.equals(country, address.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(street, city, region, postalCode, country);
    }

    @Override
    public String toString() {
        return street + ", " + city + ", " + region + ", " + country;
    }
}
