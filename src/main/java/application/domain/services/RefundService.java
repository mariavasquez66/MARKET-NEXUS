package application.domain.services;

import application.domain.enums.AuditAction;
import application.domain.enums.AuditEntityType;
import application.domain.enums.UserRole;
import application.domain.exceptions.common.UnauthorizedOperationException;
import application.domain.exceptions.refund.InvalidRefundAmountException;
import application.domain.exceptions.refund.RefundNotFoundException;
import application.domain.exceptions.returns.ReturnNotFoundException;
import application.domain.models.Refund;
import application.domain.models.Return;
import application.domain.models.User;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.RefundRepositoryPort;
import application.domain.ports.out.ReturnRepositoryPort;
import application.domain.valueobjects.Money;
import java.util.List;
import java.util.Objects;

public class RefundService {
    private final RefundRepositoryPort refundRepository;
    private final ReturnRepositoryPort returnRepository;
    private final OrderRepositoryPort orderRepository;
    private final AuditPort auditPort;
    private final ClockPort clockPort;

    public RefundService(RefundRepositoryPort refundRepository,
                         ReturnRepositoryPort returnRepository,
                         OrderRepositoryPort orderRepository,
                         AuditPort auditPort,
                         ClockPort clockPort) {
        this.refundRepository = Objects.requireNonNull(refundRepository);
        this.returnRepository = Objects.requireNonNull(returnRepository);
        this.orderRepository = Objects.requireNonNull(orderRepository);
        this.auditPort = Objects.requireNonNull(auditPort);
        this.clockPort = Objects.requireNonNull(clockPort);
    }

    public Refund generateRefund(User actor, String returnId, Money amount) {
        ServiceSupport.requireRole(actor, UserRole.ADMIN);
        Return returnRequest = returnRepository.findById(returnId)
                .orElseThrow(() -> new ReturnNotFoundException("Return not found: " + returnId));
        Money orderTotal = returnRequest.getOrder().getTotalAmount();
        Money accumulated = refundRepository.findByReturnId(returnId).stream()
                .map(Refund::getAmount)
                .reduce(Money.cop(java.math.BigDecimal.ZERO), Money::add);
        if (accumulated.add(amount).isGreaterThan(orderTotal)) {
            throw new InvalidRefundAmountException("Accumulated refunds exceed order total");
        }
        Refund refund = Refund.create(returnRequest, amount, orderTotal);
        Refund saved = refundRepository.save(refund);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.REFUND_CREATED,
                AuditEntityType.REFUND, saved.getId(), actor, "Refund created");
        return saved;
    }

    public Refund process(User actor, String refundId) {
        ServiceSupport.requireRole(actor, UserRole.ADMIN);
        Refund refund = requireRefund(refundId);
        refund.process();
        Refund saved = refundRepository.save(refund);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.REFUND_PROCESSED,
                AuditEntityType.REFUND, saved.getId(), actor, "Refund processed");
        return saved;
    }

    public List<Refund> listByReturn(User actor, String returnId) {
        ServiceSupport.requireAnyRole(actor, UserRole.BUYER, UserRole.ADMIN, UserRole.SUPERVISOR);
        List<Refund> refunds = refundRepository.findByReturnId(returnId);
        if (actor.hasRole(UserRole.BUYER) && refunds.stream().anyMatch(refund ->
                !refund.getReturnRequest().getOrder().getBuyer().getUser().getId().equals(actor.getId()))) {
            throw new UnauthorizedOperationException("Cannot access another buyer refunds");
        }
        return refunds;
    }

    public Refund getRefund(User actor, String refundId) {
        ServiceSupport.requireAnyRole(actor, UserRole.BUYER, UserRole.ADMIN, UserRole.SUPERVISOR);
        Refund refund = requireRefund(refundId);
        if (actor.hasRole(UserRole.BUYER)
                && !refund.getReturnRequest().getOrder().getBuyer().getUser().getId()
                        .equals(actor.getId())) {
            throw new UnauthorizedOperationException("Cannot access another buyer refund");
        }
        return refund;
    }

    private Refund requireRefund(String refundId) {
        return refundRepository.findById(refundId)
                .orElseThrow(() -> new RefundNotFoundException("Refund not found: " + refundId));
    }
}
