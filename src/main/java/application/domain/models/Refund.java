package application.domain.models;

import application.domain.enums.RefundStatus;
import application.domain.enums.ReturnStatus;
import application.domain.exceptions.refund.InvalidRefundAmountException;
import application.domain.exceptions.refund.RefundAlreadyProcessedException;
import application.domain.exceptions.returns.ReturnNotAllowedException;
import application.domain.valueobjects.Money;
import java.util.Objects;
import java.util.UUID;

public class Refund {
    private final String id;
    private final Return returnRequest;
    private final Money amount;
    private RefundStatus status;

    private Refund(String id, Return returnRequest, Money amount, RefundStatus status) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.returnRequest = Objects.requireNonNull(returnRequest, "returnRequest cannot be null");
        this.amount = Objects.requireNonNull(amount, "amount cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
    }

    public static Refund create(Return returnRequest, Money amount, Money orderTotal) {
        Objects.requireNonNull(returnRequest, "returnRequest cannot be null");
        if (!returnRequest.getStatus().equals(ReturnStatus.APPROVED)
                && !returnRequest.getStatus().equals(ReturnStatus.COMPLETED)) {
            throw new ReturnNotAllowedException("Refund requires an approved or completed return");
        }
        if (amount == null || !amount.isPositive()) {
            throw new InvalidRefundAmountException("Refund amount must be greater than zero");
        }
        if (orderTotal == null || !amount.isLessThanOrEqualTo(orderTotal)) {
            throw new InvalidRefundAmountException("Refund amount exceeds order total");
        }
        return new Refund(UUID.randomUUID().toString(), returnRequest, amount, RefundStatus.PENDING);
    }

    public static Refund reconstitute(String id, Return returnRequest, Money amount, RefundStatus status) {
        return new Refund(id, returnRequest, amount, status);
    }

    public void process() {
        if (!status.equals(RefundStatus.PENDING)) {
            throw new RefundAlreadyProcessedException("Refund is already processed");
        }
        status = RefundStatus.PROCESSED;
    }

    public String getId() {
        return id;
    }

    public Return getReturnRequest() {
        return returnRequest;
    }

    public Money getAmount() {
        return amount;
    }

    public RefundStatus getStatus() {
        return status;
    }
}
