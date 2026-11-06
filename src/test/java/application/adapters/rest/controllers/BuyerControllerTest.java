package application.adapters.rest.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import application.adapters.rest.mappers.BuyerRestMapper;
import application.adapters.rest.mappers.CartRestMapper;
import application.adapters.rest.mappers.InvoiceRestMapper;
import application.adapters.rest.mappers.OrderRestMapper;
import application.adapters.rest.mappers.RefundRestMapper;
import application.adapters.rest.mappers.ReturnRestMapper;
import application.domain.DomainFixtures;
import application.domain.enums.OrderStatus;
import application.domain.enums.UserRole;
import application.domain.exceptions.order.CancelledOrderException;
import application.domain.exceptions.order.OrderNotFoundException;
import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.Refund;
import application.domain.models.Return;
import application.domain.models.Seller;
import application.domain.models.User;
import application.domain.ports.in.NaturalCustomerPort;
import application.domain.valueobjects.Address;
import application.domain.valueobjects.Money;
import application.domain.valueobjects.StockAllocation;
import application.infrastructure.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BuyerControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-15T10:00:00Z");

    @Mock
    private NaturalCustomerPort naturalCustomerPort;

    private final BuyerRestMapper buyerRestMapper = new BuyerRestMapper();
    private final CartRestMapper cartRestMapper = new CartRestMapper();
    private final OrderRestMapper orderRestMapper = new OrderRestMapper();
    private final InvoiceRestMapper invoiceRestMapper = new InvoiceRestMapper();
    private final ReturnRestMapper returnRestMapper = new ReturnRestMapper();
    private final RefundRestMapper refundRestMapper = new RefundRestMapper();

    private MockMvc mockMvc;
    private User actor;
    private Buyer buyer;
    private Product product;

    @BeforeEach
    void setUp() {
        actor = DomainFixtures.user(UserRole.BUYER);
        buyer = Buyer.create(actor, DomainFixtures.address());
        product = publishedProduct();

        BuyerController controller = new BuyerController(
                naturalCustomerPort,
                buyerRestMapper,
                cartRestMapper,
                orderRestMapper,
                invoiceRestMapper,
                returnRestMapper,
                refundRestMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(RestControllerTestSupport.actorResolver())
                .build();
    }

    @Test
    void getCartReturnsBuyerCart() throws Exception {
        Cart cart = cart(2);
        when(naturalCustomerPort.getCart(actor)).thenReturn(cart);

        mockMvc.perform(get("/api/v1/buyer/cart")
                        .with(RestControllerTestSupport.actor(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cart.getId()))
                .andExpect(jsonPath("$.buyerId").value(buyer.getId()))
                .andExpect(jsonPath("$.items[0].productId").value(product.getId()))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.total.currency").value("COP"));
    }

    @Test
    void addCartItemReturnsUpdatedCart() throws Exception {
        when(naturalCustomerPort.addCartItem(actor, product.getId(), 3)).thenReturn(cart(3));

        String body = """
                {
                  "productId": "%s",
                  "quantity": 3
                }
                """.formatted(product.getId());

        mockMvc.perform(post("/api/v1/buyer/cart/items")
                        .with(RestControllerTestSupport.actor(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productId").value(product.getId()))
                .andExpect(jsonPath("$.items[0].quantity").value(3));

        verify(naturalCustomerPort).addCartItem(actor, product.getId(), 3);
    }

    @Test
    void updateCartItemReturnsUpdatedCart() throws Exception {
        when(naturalCustomerPort.updateCartItemQuantity(actor, product.getId(), 5))
                .thenReturn(cart(5));

        String body = """
                {
                  "quantity": 5
                }
                """;

        mockMvc.perform(put("/api/v1/buyer/cart/items/{productId}", product.getId())
                        .with(RestControllerTestSupport.actor(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(5));

        verify(naturalCustomerPort).updateCartItemQuantity(actor, product.getId(), 5);
    }

    @Test
    void removeCartItemReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/buyer/cart/items/{productId}", product.getId())
                        .with(RestControllerTestSupport.actor(actor)))
                .andExpect(status().isNoContent());

        verify(naturalCustomerPort).removeCartItem(actor, product.getId());
    }

    @Test
    void clearCartReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/buyer/cart")
                        .with(RestControllerTestSupport.actor(actor)))
                .andExpect(status().isNoContent());

        verify(naturalCustomerPort).clearCart(actor);
    }

    @Test
    void checkoutReturnsCreatedOrder() throws Exception {
        Order order = pendingOrder();
        when(naturalCustomerPort.checkout(eq(actor), any(Address.class))).thenReturn(order);

        String body = """
                {
                  "deliveryAddress": {
                    "street": "Calle 1 # 2-3",
                    "city": "Bogota",
                    "region": "Cundinamarca",
                    "postalCode": "110111",
                    "country": "Colombia"
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/buyer/orders")
                        .with(RestControllerTestSupport.actor(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(order.getId()))
                .andExpect(jsonPath("$.buyerId").value(buyer.getId()))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.deliveryAddress.city").value("Bogota"));
    }

    @Test
    void listOrdersWithoutStatusUsesAllOrders() throws Exception {
        Order order = pendingOrder();
        when(naturalCustomerPort.listOrders(actor)).thenReturn(List.of(order));

        mockMvc.perform(get("/api/v1/buyer/orders")
                        .with(RestControllerTestSupport.actor(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(order.getId()))
                .andExpect(jsonPath("$[0].status").value("PENDING_PAYMENT"));

        verify(naturalCustomerPort).listOrders(actor);
        verify(naturalCustomerPort, never()).listOrdersByStatus(any(), any());
    }

    @Test
    void listOrdersWithStatusFiltersByParsedCatalogCode() throws Exception {
        Order order = paidOrder();
        when(naturalCustomerPort.listOrdersByStatus(actor, OrderStatus.PAID))
                .thenReturn(List.of(order));

        mockMvc.perform(get("/api/v1/buyer/orders")
                        .param("status", "paid")
                        .with(RestControllerTestSupport.actor(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(order.getId()))
                .andExpect(jsonPath("$[0].status").value("PAID"));

        verify(naturalCustomerPort).listOrdersByStatus(actor, OrderStatus.PAID);
        verify(naturalCustomerPort, never()).listOrders(actor);
    }

    @Test
    void getOrderReturnsOrderDetail() throws Exception {
        Order order = pendingOrder();
        when(naturalCustomerPort.getOrder(actor, order.getId())).thenReturn(order);

        mockMvc.perform(get("/api/v1/buyer/orders/{id}", order.getId())
                        .with(RestControllerTestSupport.actor(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId()))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void payOrderReturnsPaidOrder() throws Exception {
        Order order = paidOrder();
        when(naturalCustomerPort.payOrder(actor, order.getId())).thenReturn(order);

        mockMvc.perform(post("/api/v1/buyer/orders/{id}/pay", order.getId())
                        .with(RestControllerTestSupport.actor(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId()))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paymentStatus").value("CONFIRMED"));

        verify(naturalCustomerPort).payOrder(actor, order.getId());
    }

    @Test
    void cancelOrderReturnsCancelledOrder() throws Exception {
        Order order = pendingOrder();
        order.cancel();
        when(naturalCustomerPort.cancelOrder(actor, order.getId())).thenReturn(order);

        mockMvc.perform(post("/api/v1/buyer/orders/{id}/cancel", order.getId())
                        .with(RestControllerTestSupport.actor(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(naturalCustomerPort).cancelOrder(actor, order.getId());
    }

    @Test
    void finalizeOrderReturnsFinalizedOrder() throws Exception {
        Order order = deliveredOrder();
        order.finalizeOrder();
        when(naturalCustomerPort.finalizeOrder(actor, order.getId())).thenReturn(order);

        mockMvc.perform(post("/api/v1/buyer/orders/{id}/finalize", order.getId())
                        .with(RestControllerTestSupport.actor(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId()))
                .andExpect(jsonPath("$.status").value("FINALIZED"));

        verify(naturalCustomerPort).finalizeOrder(actor, order.getId());
    }

    @Test
    void listAddressesReturnsMainAndAdditionalAddresses() throws Exception {
        Buyer buyerWithAddresses = Buyer.create(actor, DomainFixtures.address());
        buyerWithAddresses.addAdditionalAddress(
                Address.of("Calle 9", "Medellin", "Antioquia", null, "Colombia"));
        when(naturalCustomerPort.listAddresses(actor))
                .thenReturn(List.of(buyerWithAddresses.getMainAddress(),
                        buyerWithAddresses.getAdditionalAddresses().get(0)));

        mockMvc.perform(get("/api/v1/buyer/addresses")
                        .with(RestControllerTestSupport.actor(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].city").value("Bogota"))
                .andExpect(jsonPath("$[1].city").value("Medellin"));

        verify(naturalCustomerPort).listAddresses(actor);
    }

    @Test
    void getInvoiceReturnsInvoice() throws Exception {
        Order order = deliveredOrder();
        when(naturalCustomerPort.getInvoice(actor, order.getId()))
                .thenReturn(order.getInvoice());

        mockMvc.perform(get("/api/v1/buyer/orders/{id}/invoice", order.getId())
                        .with(RestControllerTestSupport.actor(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getInvoice().getId()))
                .andExpect(jsonPath("$.orderId").value(order.getId()))
                .andExpect(jsonPath("$.number").value("INV-001"))
                .andExpect(jsonPath("$.status").value("ISSUED"));
    }

    @Test
    void requestReturnReturnsCreatedReturn() throws Exception {
        Order order = deliveredOrder();
        Return returnRequest = Return.request(order, "Broken item", CREATED_AT.plusSeconds(1200));
        when(naturalCustomerPort.requestReturn(actor, order.getId(), "Broken item"))
                .thenReturn(returnRequest);

        String body = """
                {
                  "reason": "Broken item"
                }
                """;

        mockMvc.perform(post("/api/v1/buyer/orders/{id}/returns", order.getId())
                        .with(RestControllerTestSupport.actor(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(returnRequest.getId()))
                .andExpect(jsonPath("$.orderId").value(order.getId()))
                .andExpect(jsonPath("$.reason").value("Broken item"))
                .andExpect(jsonPath("$.status").value("REQUESTED"));
    }

    @Test
    void listReturnsReturnsBuyerReturns() throws Exception {
        Return returnRequest = returnRequest();
        when(naturalCustomerPort.listReturns(actor)).thenReturn(List.of(returnRequest));

        mockMvc.perform(get("/api/v1/buyer/returns")
                        .with(RestControllerTestSupport.actor(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(returnRequest.getId()))
                .andExpect(jsonPath("$[0].reason").value("Broken item"))
                .andExpect(jsonPath("$[0].status").value("REQUESTED"));
    }

    @Test
    void listRefundsReturnsBuyerRefunds() throws Exception {
        Refund refund = refund();
        when(naturalCustomerPort.listRefunds(actor)).thenReturn(List.of(refund));

        mockMvc.perform(get("/api/v1/buyer/refunds")
                        .with(RestControllerTestSupport.actor(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(refund.getId()))
                .andExpect(jsonPath("$[0].returnId").value(refund.getReturnRequest().getId()))
                .andExpect(jsonPath("$[0].amount.amount").value(50.00))
                .andExpect(jsonPath("$[0].amount.currency").value("COP"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void addAddressReturnsUpdatedBuyer() throws Exception {
        Address additional = Address.of("Calle 9", "Medellin", "Antioquia", null, "Colombia");
        buyer.addAdditionalAddress(additional);
        when(naturalCustomerPort.addAddress(eq(actor), any(Address.class))).thenReturn(buyer);

        String body = """
                {
                  "address": {
                    "street": "Calle 9",
                    "city": "Medellin",
                    "region": "Antioquia",
                    "country": "Colombia"
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/buyer/addresses")
                        .with(RestControllerTestSupport.actor(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(buyer.getId()))
                .andExpect(jsonPath("$.mainAddress.city").value("Bogota"))
                .andExpect(jsonPath("$.additionalAddresses[0].city").value("Medellin"));
    }

    @Test
    void changeMainAddressReturnsUpdatedBuyer() throws Exception {
        buyer.changeMainAddress(
                Address.of("Calle 10", "Cali", "Valle", null, "Colombia"));
        when(naturalCustomerPort.changeMainAddress(eq(actor), any(Address.class)))
                .thenReturn(buyer);

        String body = """
                {
                  "address": {
                    "street": "Calle 10",
                    "city": "Cali",
                    "region": "Valle",
                    "country": "Colombia"
                  }
                }
                """;

        mockMvc.perform(put("/api/v1/buyer/addresses/main")
                        .with(RestControllerTestSupport.actor(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(buyer.getId()))
                .andExpect(jsonPath("$.mainAddress.city").value("Cali"));
    }

    @Test
    void addCartItemRejectsNonPositiveQuantity() throws Exception {
        String body = """
                {
                  "productId": "%s",
                  "quantity": 0
                }
                """.formatted(product.getId());

        mockMvc.perform(post("/api/v1/buyer/cart/items")
                        .with(RestControllerTestSupport.actor(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(naturalCustomerPort);
    }

    @Test
    void getMissingOrderReturnsNotFound() throws Exception {
        when(naturalCustomerPort.getOrder(actor, "missing"))
                .thenThrow(new OrderNotFoundException("Order not found"));

        mockMvc.perform(get("/api/v1/buyer/orders/missing")
                        .with(RestControllerTestSupport.actor(actor)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void cancelOrderConflictReturnsConflict() throws Exception {
        when(naturalCustomerPort.cancelOrder(actor, "order-1"))
                .thenThrow(new CancelledOrderException("Order is cancelled"));

        mockMvc.perform(post("/api/v1/buyer/orders/order-1/cancel")
                        .with(RestControllerTestSupport.actor(actor)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CANCELLED_ORDER"));
    }

    @Test
    void invalidStatusQueryReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/buyer/orders")
                        .param("status", "NOT_A_STATUS")
                        .with(RestControllerTestSupport.actor(actor)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATA"));

        verifyNoInteractions(naturalCustomerPort);
    }

    private Cart cart(int quantity) {
        Cart cart = Cart.create(buyer);
        cart.addItem(product, quantity);
        return cart;
    }

    private Order pendingOrder() {
        return Order.fromCart(cart(2), DomainFixtures.address(), CREATED_AT);
    }

    private Order paidOrder() {
        Order order = pendingOrder();
        order.assignAllocations(List.of(
                StockAllocation.of(product.getId(), "warehouse-1", 2)));
        order.confirmPayment(CREATED_AT.plusSeconds(300));
        return order;
    }

    private Order deliveredOrder() {
        Order order = paidOrder();
        Invoice invoice = Invoice.create(order, "INV-001", new BigDecimal("0.19"),
                CREATED_AT.plusSeconds(600));
        order.attachInvoice(invoice);
        order.markDispatched();
        order.markDelivered();
        return order;
    }

    private Return returnRequest() {
        return Return.request(deliveredOrder(), "Broken item", CREATED_AT.plusSeconds(1200));
    }

    private Refund refund() {
        Return request = returnRequest();
        request.approve();
        return Refund.create(request, Money.cop(new BigDecimal("50.00")),
                request.getOrder().getTotalAmount());
    }

    private static Product publishedProduct() {
        Seller seller = DomainFixtures.seller(DomainFixtures.admin());
        return DomainFixtures.publishedPhysicalProduct(seller, DomainFixtures.operator());
    }
}
