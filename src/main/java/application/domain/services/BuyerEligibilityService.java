package application.domain.services;

import application.domain.enums.UserRole;
import application.domain.exceptions.buyer.BuyerNotFoundException;
import application.domain.models.Buyer;
import application.domain.models.User;
import application.domain.ports.out.BuyerRepositoryPort;
import java.util.Objects;

public class BuyerEligibilityService {
    private final BuyerRepositoryPort buyerRepository;

    public BuyerEligibilityService(BuyerRepositoryPort buyerRepository) {
        this.buyerRepository = Objects.requireNonNull(buyerRepository);
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
