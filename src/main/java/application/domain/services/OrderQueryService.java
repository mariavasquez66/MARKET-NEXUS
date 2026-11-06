package application.domain.services;

import application.domain.enums.OrderStatus;
import application.domain.enums.UserRole;
import application.domain.exceptions.buyer.BuyerNotFoundException;
import application.domain.exceptions.common.UnauthorizedOperationException;
import application.domain.exceptions.order.OrderNotFoundException;
import application.domain.models.Buyer;
import application.domain.models.Order;
import application.domain.models.User;
import application.domain.ports.out.BuyerRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import java.util.List;
import java.util.Objects;

public class OrderQueryService {
    private final BuyerRepositoryPort buyerRepository;
    private final OrderRepositoryPort orderRepository;

    public OrderQueryService(BuyerRepositoryPort buyerRepository,
                             OrderRepositoryPort orderRepository) {
        this.buyerRepository = Objects.requireNonNull(buyerRepository);
        this.orderRepository = Objects.requireNonNull(orderRepository);
    }

    public Order getOrder(User actor, String orderId) {
        ServiceSupport.requireAnyRole(actor, UserRole.BUYER, UserRole.SELLER,
                UserRole.LOGISTIC_OPERATOR, UserRole.SUPERVISOR, UserRole.ADMIN);
        Order order = requireOrder(orderId);
        if (canView(actor, order)) {
            return order;
        }
        throw new UnauthorizedOperationException("Cannot access this order");
    }

    public List<Order> listOrders(User actor) {
        return listOrders(actor, null);
    }

    public List<Order> listByStatus(User actor, OrderStatus status) {
        Objects.requireNonNull(status, "status cannot be null");
        return listOrders(actor, status);
    }

    private List<Order> listOrders(User actor, OrderStatus status) {
        ServiceSupport.requireAnyRole(actor, UserRole.BUYER, UserRole.SELLER,
                UserRole.LOGISTIC_OPERATOR, UserRole.SUPERVISOR, UserRole.ADMIN);
        if (actor.hasRole(UserRole.BUYER)) {
            Buyer buyer = buyerRepository.findByUserId(actor.getId())
                    .orElseThrow(() -> new BuyerNotFoundException("Buyer not found for current user"));
            return status == null
                    ? orderRepository.findByBuyerId(buyer.getId())
                    : orderRepository.findByBuyerIdAndStatus(buyer.getId(), status);
        }
        List<Order> orders = status == null ? orderRepository.findAll() : orderRepository.findByStatus(status);
        if (actor.hasRole(UserRole.SELLER)) {
            return orders.stream().filter(order -> belongsToSeller(order, actor.getId())).toList();
        }
        return orders;
    }

    private boolean canView(User actor, Order order) {
        if (actor.hasRole(UserRole.ADMIN) || actor.hasRole(UserRole.SUPERVISOR)
                || actor.hasRole(UserRole.LOGISTIC_OPERATOR)) {
            return true;
        }
        if (actor.hasRole(UserRole.BUYER)) {
            return order.getBuyer().getUser().getId().equals(actor.getId());
        }
        return belongsToSeller(order, actor.getId());
    }

    private boolean belongsToSeller(Order order, String userId) {
        return order.getItems().stream()
                .anyMatch(item -> item.getProduct().getSeller().getUser().getId().equals(userId));
    }

    private Order requireOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }
}
