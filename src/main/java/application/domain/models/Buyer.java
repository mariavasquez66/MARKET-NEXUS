package application.domain.models;

import application.domain.enums.CommercialStatus;
import application.domain.enums.UserRole;
import application.domain.exceptions.buyer.RestrictedBuyerException;
import application.domain.exceptions.common.InvalidDataException;
import application.domain.exceptions.user.InvalidRoleException;
import application.domain.valueobjects.Address;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Buyer {
    private final String id;
    private final User user;
    private Address mainAddress;
    private final List<Address> additionalAddresses;
    private CommercialStatus commercialStatus;

    private Buyer(String id,
                  User user,
                  Address mainAddress,
                  List<Address> additionalAddresses,
                  CommercialStatus commercialStatus) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.user = Objects.requireNonNull(user, "user cannot be null");
        this.mainAddress = Objects.requireNonNull(mainAddress, "mainAddress cannot be null");
        this.additionalAddresses = new ArrayList<>(Objects.requireNonNull(additionalAddresses));
        this.commercialStatus = Objects.requireNonNull(commercialStatus, "commercialStatus cannot be null");
    }

    public static Buyer create(User user, Address mainAddress) {
        if (!user.hasRole(UserRole.BUYER)) {
            throw new InvalidRoleException("Buyer user must have BUYER role");
        }
        return new Buyer(UUID.randomUUID().toString(), user, mainAddress,
                List.of(), CommercialStatus.ELIGIBLE);
    }

    public static Buyer reconstitute(String id,
                                     User user,
                                     Address mainAddress,
                                     List<Address> additionalAddresses,
                                     CommercialStatus commercialStatus) {
        return new Buyer(id, user, mainAddress, additionalAddresses, commercialStatus);
    }

    public void addAdditionalAddress(Address address) {
        Objects.requireNonNull(address, "address cannot be null");
        if (mainAddress.equals(address) || additionalAddresses.contains(address)) {
            throw new InvalidDataException("Address already registered");
        }
        additionalAddresses.add(address);
    }

    public void removeAdditionalAddress(Address address) {
        if (!additionalAddresses.remove(address)) {
            throw new InvalidDataException("Address is not registered");
        }
    }

    public void changeMainAddress(Address address) {
        this.mainAddress = Objects.requireNonNull(address, "address cannot be null");
    }

    public void restrict() {
        commercialStatus = CommercialStatus.RESTRICTED;
    }

    public void suspend() {
        commercialStatus = CommercialStatus.SUSPENDED;
    }

    public void markEligible() {
        commercialStatus = CommercialStatus.ELIGIBLE;
    }

    public void assertCanPurchase() {
        if (!isEligible()) {
            throw new RestrictedBuyerException("Buyer is not eligible to purchase: " + id);
        }
    }

    public boolean isEligible() {
        return commercialStatus.equals(CommercialStatus.ELIGIBLE);
    }

    public String getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Address getMainAddress() {
        return mainAddress;
    }

    public List<Address> getAdditionalAddresses() {
        return Collections.unmodifiableList(additionalAddresses);
    }

    public CommercialStatus getCommercialStatus() {
        return commercialStatus;
    }
}
