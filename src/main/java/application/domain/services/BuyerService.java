package application.domain.services;

import application.domain.enums.UserRole;
import application.domain.exceptions.buyer.BuyerNotFoundException;
import application.domain.exceptions.common.UnauthorizedOperationException;
import application.domain.models.Buyer;
import application.domain.models.User;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.BuyerRepositoryPort;
import application.domain.ports.out.ClockPort;
import application.domain.valueobjects.Address;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BuyerService {
    private final BuyerRepositoryPort buyerRepository;
    private final AuditPort auditPort;
    private final ClockPort clockPort;

    public BuyerService(BuyerRepositoryPort buyerRepository,
                        AuditPort auditPort,
                        ClockPort clockPort) {
        this.buyerRepository = Objects.requireNonNull(buyerRepository);
        this.auditPort = Objects.requireNonNull(auditPort);
        this.clockPort = Objects.requireNonNull(clockPort);
    }

    public Buyer getBuyer(User actor, String buyerId) {
        ServiceSupport.requireAnyRole(actor, UserRole.BUYER, UserRole.ADMIN);
        Buyer buyer = requireBuyer(buyerId);
        if (!actor.hasRole(UserRole.ADMIN) && !buyer.getUser().getId().equals(actor.getId())) {
            throw new UnauthorizedOperationException("Cannot access another buyer");
        }
        return buyer;
    }

    public Buyer getBuyerByUser(User actor) {
        ServiceSupport.requireRole(actor, UserRole.BUYER);
        return buyerRepository.findByUserId(actor.getId())
                .orElseThrow(() -> new BuyerNotFoundException("Buyer not found for current user"));
    }

    public List<Address> listAddresses(User actor) {
        Buyer buyer = getBuyerByUser(actor);
        List<Address> addresses = new ArrayList<>();
        addresses.add(buyer.getMainAddress());
        addresses.addAll(buyer.getAdditionalAddresses());
        return List.copyOf(addresses);
    }

    public Buyer addAdditionalAddress(User actor, Address address) {
        Buyer buyer = getBuyerByUser(actor);
        buyer.addAdditionalAddress(address);
        return buyerRepository.save(buyer);
    }

    public Buyer changeMainAddress(User actor, Address address) {
        Buyer buyer = getBuyerByUser(actor);
        buyer.changeMainAddress(address);
        return buyerRepository.save(buyer);
    }

    public Buyer restrict(User actor, String buyerId) {
        ServiceSupport.requireRole(actor, UserRole.ADMIN);
        Buyer buyer = requireBuyer(buyerId);
        buyer.restrict();
        return buyerRepository.save(buyer);
    }

    public Buyer suspend(User actor, String buyerId) {
        ServiceSupport.requireRole(actor, UserRole.ADMIN);
        Buyer buyer = requireBuyer(buyerId);
        buyer.suspend();
        return buyerRepository.save(buyer);
    }

    public Buyer markEligible(User actor, String buyerId) {
        ServiceSupport.requireRole(actor, UserRole.ADMIN);
        Buyer buyer = requireBuyer(buyerId);
        buyer.markEligible();
        return buyerRepository.save(buyer);
    }

    private Buyer requireBuyer(String buyerId) {
        return buyerRepository.findById(buyerId)
                .orElseThrow(() -> new BuyerNotFoundException("Buyer not found: " + buyerId));
    }
}
