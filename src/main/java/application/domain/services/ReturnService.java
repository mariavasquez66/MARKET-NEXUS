package application.domain.services;

import application.domain.enums.AuditAction;
import application.domain.enums.AuditEntityType;
import application.domain.enums.UserRole;
import application.domain.exceptions.common.InvalidDataException;
import application.domain.exceptions.common.UnauthorizedOperationException;
import application.domain.exceptions.order.OrderNotFoundException;
import application.domain.exceptions.returns.ReturnNotFoundException;
import application.domain.models.Order;
import application.domain.models.Return;
import application.domain.models.User;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.ReturnRepositoryPort;
import application.domain.valueobjects.StockAllocation;
import java.util.List;
import java.util.Objects;

public class ReturnService {
    private final ReturnRepositoryPort returnRepository;
    private final OrderRepositoryPort orderRepository;
    private final InventoryService inventoryService;
    private final AuditPort auditPort;
    private final ClockPort clockPort;

    public ReturnService(ReturnRepositoryPort returnRepository,
                         OrderRepositoryPort orderRepository,
                         InventoryService inventoryService,
                         AuditPort auditPort,
                         ClockPort clockPort) {
        this.returnRepository = Objects.requireNonNull(returnRepository);
        this.orderRepository = Objects.requireNonNull(orderRepository);
        this.inventoryService = Objects.requireNonNull(inventoryService);
        this.auditPort = Objects.requireNonNull(auditPort);
        this.clockPort = Objects.requireNonNull(clockPort);
    }

    public Return requestReturn(User actor, String orderId, String reason) {
        return requestReturn(actor, orderId, reason, null, null);
    }

    public Return requestReturn(User actor,
                                String orderId,
                                String reason,
                                String productId,
                                Integer quantity) {
        ServiceSupport.requireRole(actor, UserRole.BUYER);
        if (reason == null || reason.isBlank()) {
            throw new InvalidDataException("Return reason is required");
        }
        Order order = requireOrder(orderId);
        if (!order.getBuyer().getUser().getId().equals(actor.getId())) {
            throw new UnauthorizedOperationException("Order does not belong to current buyer");
        }
        Return returnRequest = returnRepository.save(
                Return.request(order, reason, clockPort.now(), productId, quantity));
        ServiceSupport.audit(auditPort, clockPort, AuditAction.RETURN_REQUESTED,
                AuditEntityType.RETURN, returnRequest.getId(), actor, "Return requested");
        return returnRequest;
    }

    public Return approve(User actor, String returnId) {
        ServiceSupport.requireRole(actor, UserRole.ADMIN);
        Return returnRequest = requireReturn(returnId);
        returnRequest.approve();
        Return saved = returnRepository.save(returnRequest);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.RETURN_APPROVED,
                AuditEntityType.RETURN, saved.getId(), actor, "Return approved");
        return saved;
    }

    public Return reject(User actor, String returnId) {
        ServiceSupport.requireRole(actor, UserRole.ADMIN);
        Return returnRequest = requireReturn(returnId);
        returnRequest.reject();
        Return saved = returnRepository.save(returnRequest);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.RETURN_REJECTED,
                AuditEntityType.RETURN, saved.getId(), actor, "Return rejected");
        return saved;
    }

    public Return complete(User actor, String returnId) {
        ServiceSupport.requireRole(actor, UserRole.LOGISTIC_OPERATOR);
        Return returnRequest = requireReturn(returnId);
        returnRequest.complete();
        Order order = returnRequest.getOrder();
        if (returnRequest.getProductId() == null) {
            for (StockAllocation allocation : order.getAllocations()) {
                inventoryService.registerReturn(actor, allocation.getProductId(),
                        allocation.getWarehouseId(), allocation.getQuantity());
            }
        } else {
            registerSelectedProductReturn(actor, order, returnRequest);
        }
        Return saved = returnRepository.save(returnRequest);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.RETURN_COMPLETED,
                AuditEntityType.RETURN, saved.getId(), actor, "Return completed");
        return saved;
    }

    private void registerSelectedProductReturn(User actor, Order order, Return returnRequest) {
        int remaining = returnRequest.getQuantity();
        for (StockAllocation allocation : order.getAllocations()) {
            if (remaining == 0) {
                break;
            }
            if (!allocation.getProductId().equals(returnRequest.getProductId())) {
                continue;
            }
            int quantity = Math.min(remaining, allocation.getQuantity());
            inventoryService.registerReturn(actor, allocation.getProductId(),
                    allocation.getWarehouseId(), quantity);
            remaining -= quantity;
        }
        if (remaining > 0) {
            throw new InvalidDataException("Return quantity exceeds shipped quantity");
        }
    }

    public List<Return> listReturns(User actor) {
        ServiceSupport.requireAnyRole(actor, UserRole.BUYER, UserRole.ADMIN,
                UserRole.LOGISTIC_OPERATOR, UserRole.SUPERVISOR);
        List<Return> returns = returnRepository.findAll();
        if (actor.hasRole(UserRole.BUYER)) {
            return returns.stream()
                    .filter(returnRequest -> returnRequest.getOrder().getBuyer().getUser().getId()
                            .equals(actor.getId()))
                    .toList();
        }
        return returns;
    }

    public Return getReturn(User actor, String returnId) {
        ServiceSupport.requireAnyRole(actor, UserRole.BUYER, UserRole.ADMIN,
                UserRole.LOGISTIC_OPERATOR, UserRole.SUPERVISOR);
        Return returnRequest = requireReturn(returnId);
        if (actor.hasRole(UserRole.BUYER)
                && !returnRequest.getOrder().getBuyer().getUser().getId().equals(actor.getId())) {
            throw new UnauthorizedOperationException("Cannot access another buyer return");
        }
        return returnRequest;
    }

    private Return requireReturn(String returnId) {
        return returnRepository.findById(returnId)
                .orElseThrow(() -> new ReturnNotFoundException("Return not found: " + returnId));
    }

    private Order requireOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }
}
