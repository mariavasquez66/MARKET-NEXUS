package application.domain.services;

import application.domain.enums.AuditAction;
import application.domain.enums.InventoryCondition;
import application.domain.enums.OrderStatus;
import application.domain.enums.PaymentStatus;
import application.domain.enums.ReturnStatus;
import application.domain.enums.UserRole;
import application.domain.exceptions.common.InvalidDateRangeException;
import application.domain.exceptions.common.NoDataException;
import application.domain.models.AuditLog;
import application.domain.models.Inventory;
import application.domain.models.Order;
import application.domain.models.Refund;
import application.domain.models.Return;
import application.domain.models.User;
import application.domain.models.reports.InventorySummary;
import application.domain.models.reports.OrderSummary;
import application.domain.models.reports.ReturnSummary;
import application.domain.models.reports.SalesReport;
import application.domain.models.reports.UserSummary;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.InventoryRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.ReturnRepositoryPort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.valueobjects.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ReportService {
    private final OrderRepositoryPort orderRepository;
    private final InventoryRepositoryPort inventoryRepository;
    private final ReturnRepositoryPort returnRepository;
    private final UserRepositoryPort userRepository;
    private final AuditPort auditPort;
    private final ClockPort clockPort;

    public ReportService(OrderRepositoryPort orderRepository,
                         InventoryRepositoryPort inventoryRepository,
                         ReturnRepositoryPort returnRepository,
                         UserRepositoryPort userRepository,
                         AuditPort auditPort,
                         ClockPort clockPort) {
        this.orderRepository = Objects.requireNonNull(orderRepository);
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository);
        this.returnRepository = Objects.requireNonNull(returnRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
        this.auditPort = Objects.requireNonNull(auditPort);
        this.clockPort = Objects.requireNonNull(clockPort);
    }

    public SalesReport salesReport(User actor, LocalDate from, LocalDate to) {
        ServiceSupport.requireRole(actor, UserRole.SUPERVISOR);
        validateRange(from, to);
        Instant start = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<Order> orders = orderRepository.findByPaymentStatusAndCreatedAtBetween(
                PaymentStatus.CONFIRMED, start, end);
        if (orders.isEmpty()) {
            throw new NoDataException("No sales data for the selected range");
        }
        Money total = orders.stream()
                .map(Order::getTotalAmount)
                .reduce(Money.cop(BigDecimal.ZERO), Money::add);
        return new SalesReport(from, to, orders.size(), total);
    }

    public OrderSummary orderSummary(User actor) {
        ServiceSupport.requireRole(actor, UserRole.SUPERVISOR);
        List<Order> orders = orderRepository.findAll();
        if (orders.isEmpty()) {
            throw new NoDataException("No order data");
        }
        return new OrderSummary(
                orders.size(),
                (int) countStatus(orders, OrderStatus.PENDING_PAYMENT),
                (int) countStatus(orders, OrderStatus.PAID),
                (int) countStatus(orders, OrderStatus.DISPATCHED),
                (int) countStatus(orders, OrderStatus.DELIVERED),
                (int) countStatus(orders, OrderStatus.FINALIZED),
                (int) countStatus(orders, OrderStatus.CANCELLED));
    }

    public InventorySummary inventorySummary(User actor) {
        ServiceSupport.requireRole(actor, UserRole.SUPERVISOR);
        List<Inventory> inventories = inventoryRepository.findAll();
        if (inventories.isEmpty()) {
            throw new NoDataException("No inventory data");
        }
        int available = inventories.stream().mapToInt(Inventory::getAvailableQuantity).sum();
        int reserved = inventories.stream().mapToInt(Inventory::getReservedQuantity).sum();
        int damaged = (int) inventories.stream()
                .filter(inventory -> inventory.getCondition().equals(InventoryCondition.DAMAGED))
                .count();
        return new InventorySummary(inventories.size(), available, reserved, damaged);
    }

    public ReturnSummary returnSummary(User actor) {
        ServiceSupport.requireRole(actor, UserRole.SUPERVISOR);
        List<Return> returns = returnRepository.findAll();
        if (returns.isEmpty()) {
            throw new NoDataException("No return data");
        }
        return new ReturnSummary(
                returns.size(),
                (int) countReturnStatus(returns, ReturnStatus.REQUESTED),
                (int) countReturnStatus(returns, ReturnStatus.APPROVED),
                (int) countReturnStatus(returns, ReturnStatus.REJECTED),
                (int) countReturnStatus(returns, ReturnStatus.COMPLETED));
    }

    public UserSummary userSummary(User actor) {
        ServiceSupport.requireRole(actor, UserRole.SUPERVISOR);
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            throw new NoDataException("No user data");
        }
        Map<UserRole, Integer> byRole = new HashMap<>();
        for (User user : users) {
            byRole.merge(user.getRole(), 1, Integer::sum);
        }
        return new UserSummary(users.size(), byRole);
    }

    public List<AuditLog> auditLogs(User actor, AuditAction action, LocalDate from, LocalDate to) {
        ServiceSupport.requireRole(actor, UserRole.SUPERVISOR);
        if (from != null && to != null) {
            validateRange(from, to);
        }
        return auditPort.find(action, from, to);
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new InvalidDateRangeException("Date range is required");
        }
        if (from.isAfter(to)) {
            throw new InvalidDateRangeException("From date must not be after to date");
        }
    }

    private long countStatus(List<Order> orders, OrderStatus status) {
        return orders.stream().filter(order -> order.getStatus().equals(status)).count();
    }

    private long countReturnStatus(List<Return> returns, ReturnStatus status) {
        return returns.stream().filter(returnRequest -> returnRequest.getStatus().equals(status)).count();
    }
}
