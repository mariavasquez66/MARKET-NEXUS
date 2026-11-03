package application.adapters.useCases;

import application.domain.enums.OrderStatus;
import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.Refund;
import application.domain.models.Return;
import application.domain.models.User;
import application.domain.ports.in.NaturalCustomerPort;
import application.domain.services.BuyerService;
import application.domain.services.CartService;
import application.domain.services.InvoiceService;
import application.domain.services.OrderService;
import application.domain.services.RefundService;
import application.domain.services.ReturnService;
import application.domain.valueobjects.Address;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NaturalCustomerUseCase implements NaturalCustomerPort {

    private final CartService cartService;
    private final OrderService orderService;
    private final InvoiceService invoiceService;
    private final ReturnService returnService;
    private final RefundService refundService;
    private final BuyerService buyerService;

    public NaturalCustomerUseCase(CartService cartService,
                                  OrderService orderService,
                                  InvoiceService invoiceService,
                                  ReturnService returnService,
                                  RefundService refundService,
                                  BuyerService buyerService) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.invoiceService = invoiceService;
        this.returnService = returnService;
        this.refundService = refundService;
        this.buyerService = buyerService;
    }

    @Override
    public Cart getCart(User actor) {
        return cartService.getCart(actor);
    }

    @Override
    public Cart addCartItem(User actor, String productId, int quantity) {
        return cartService.addItem(actor, productId, quantity);
    }

    @Override
    public Cart updateCartItemQuantity(User actor, String productId, int quantity) {
        return cartService.updateQuantity(actor, productId, quantity);
    }

    @Override
    public Cart removeCartItem(User actor, String productId) {
        return cartService.removeItem(actor, productId);
    }

    @Override
    public void clearCart(User actor) {
        cartService.clearCart(actor);
    }

    @Override
    public Order checkout(User actor, Address deliveryAddress) {
        return orderService.checkout(actor, deliveryAddress);
    }

    @Override
    public List<Order> listOrders(User actor) {
        return orderService.listOrders(actor);
    }

    @Override
    public List<Order> listOrdersByStatus(User actor, OrderStatus status) {
        return orderService.listByStatus(actor, status);
    }

    @Override
    public Order getOrder(User actor, String orderId) {
        return orderService.getOrder(actor, orderId);
    }

    @Override
    public Order payOrder(User actor, String orderId) {
        return orderService.confirmPayment(actor, orderId);
    }

    @Override
    public Order cancelOrder(User actor, String orderId) {
        return orderService.cancel(actor, orderId);
    }

    @Override
    public Invoice getInvoice(User actor, String orderId) {
        return invoiceService.getByOrder(actor, orderId);
    }

    @Override
    public Return requestReturn(User actor, String orderId, String reason) {
        return returnService.requestReturn(actor, orderId, reason);
    }

    @Override
    public List<Return> listReturns(User actor) {
        return returnService.listReturns(actor);
    }

    @Override
    public List<Refund> listRefunds(User actor) {
        return refundService.listMyRefunds(actor);
    }

    @Override
    public Buyer addAddress(User actor, Address address) {
        return buyerService.addAdditionalAddress(actor, address);
    }

    @Override
    public Buyer changeMainAddress(User actor, Address address) {
        return buyerService.changeMainAddress(actor, address);
    }
}
