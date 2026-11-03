package application.domain.ports.in;

import application.domain.enums.OrderStatus;
import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.Refund;
import application.domain.models.Return;
import application.domain.models.User;
import application.domain.valueobjects.Address;
import java.util.List;

public interface NaturalCustomerPort {

    Cart getCart(User actor);

    Cart addCartItem(User actor, String productId, int quantity);

    Cart updateCartItemQuantity(User actor, String productId, int quantity);

    Cart removeCartItem(User actor, String productId);

    void clearCart(User actor);

    Order checkout(User actor, Address deliveryAddress);

    List<Order> listOrders(User actor);

    List<Order> listOrdersByStatus(User actor, OrderStatus status);

    Order getOrder(User actor, String orderId);

    Order payOrder(User actor, String orderId);

    Order cancelOrder(User actor, String orderId);

    Invoice getInvoice(User actor, String orderId);

    Return requestReturn(User actor, String orderId, String reason);

    List<Return> listReturns(User actor);

    List<Refund> listRefunds(User actor);

    Buyer addAddress(User actor, Address address);

    Buyer changeMainAddress(User actor, Address address);
}
