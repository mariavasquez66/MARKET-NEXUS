package application.adapters.rest.controllers;

import application.adapters.rest.dto.request.AddAddressRequest;
import application.adapters.rest.dto.request.AddCartItemRequest;
import application.adapters.rest.dto.request.AddressRequest;
import application.adapters.rest.dto.request.ChangeMainAddressRequest;
import application.adapters.rest.dto.request.CheckoutRequest;
import application.adapters.rest.dto.request.RequestReturnRequest;
import application.adapters.rest.dto.request.UpdateCartItemRequest;
import application.adapters.rest.dto.response.AddressResponse;
import application.adapters.rest.dto.response.BuyerResponse;
import application.adapters.rest.dto.response.CartResponse;
import application.adapters.rest.dto.response.InvoiceResponse;
import application.adapters.rest.dto.response.OrderResponse;
import application.adapters.rest.dto.response.RefundResponse;
import application.adapters.rest.dto.response.ReturnResponse;
import application.adapters.rest.mappers.BuyerRestMapper;
import application.adapters.rest.mappers.CartRestMapper;
import application.adapters.rest.mappers.InvoiceRestMapper;
import application.adapters.rest.mappers.OrderRestMapper;
import application.adapters.rest.mappers.RefundRestMapper;
import application.adapters.rest.mappers.ReturnRestMapper;
import application.domain.enums.OrderStatus;
import application.domain.exceptions.common.InvalidDataException;
import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.Refund;
import application.domain.models.Return;
import application.domain.models.User;
import application.domain.ports.in.NaturalCustomerPort;
import application.domain.valueobjects.Address;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/buyer")
@Validated
public class BuyerController {

    private final NaturalCustomerPort naturalCustomerPort;
    private final BuyerRestMapper buyerRestMapper;
    private final CartRestMapper cartRestMapper;
    private final OrderRestMapper orderRestMapper;
    private final InvoiceRestMapper invoiceRestMapper;
    private final ReturnRestMapper returnRestMapper;
    private final RefundRestMapper refundRestMapper;

    public BuyerController(NaturalCustomerPort naturalCustomerPort,
                           BuyerRestMapper buyerRestMapper,
                           CartRestMapper cartRestMapper,
                           OrderRestMapper orderRestMapper,
                           InvoiceRestMapper invoiceRestMapper,
                           ReturnRestMapper returnRestMapper,
                           RefundRestMapper refundRestMapper) {
        this.naturalCustomerPort = naturalCustomerPort;
        this.buyerRestMapper = buyerRestMapper;
        this.cartRestMapper = cartRestMapper;
        this.orderRestMapper = orderRestMapper;
        this.invoiceRestMapper = invoiceRestMapper;
        this.returnRestMapper = returnRestMapper;
        this.refundRestMapper = refundRestMapper;
    }

    @GetMapping("/cart")
    public CartResponse getCart(@AuthenticationPrincipal(expression = "user") User actor) {
        return cartRestMapper.toResponse(naturalCustomerPort.getCart(actor));
    }

    @PostMapping("/cart/items")
    public CartResponse addCartItem(@Valid @RequestBody AddCartItemRequest request,
                                    @AuthenticationPrincipal(expression = "user") User actor) {
        Cart cart = naturalCustomerPort.addCartItem(actor, request.productId(), request.quantity());
        return cartRestMapper.toResponse(cart);
    }

    @PutMapping("/cart/items/{productId}")
    public CartResponse updateCartItem(@PathVariable String productId,
                                       @Valid @RequestBody UpdateCartItemRequest request,
                                       @AuthenticationPrincipal(expression = "user") User actor) {
        Cart cart = naturalCustomerPort.updateCartItemQuantity(
                actor, productId, request.quantity());
        return cartRestMapper.toResponse(cart);
    }

    @DeleteMapping("/cart/items/{productId}")
    public ResponseEntity<Void> removeCartItem(@PathVariable String productId,
                                               @AuthenticationPrincipal(expression = "user") User actor) {
        naturalCustomerPort.removeCartItem(actor, productId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/cart")
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal(expression = "user") User actor) {
        naturalCustomerPort.clearCart(actor);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> checkout(@Valid @RequestBody CheckoutRequest request,
                                                  @AuthenticationPrincipal(expression = "user") User actor) {
        Order order = naturalCustomerPort.checkout(actor, toAddress(request.deliveryAddress()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderRestMapper.toResponse(order));
    }

    @GetMapping("/orders")
    public List<OrderResponse> listOrders(
            @RequestParam(name = "status", required = false) String status,
            @AuthenticationPrincipal(expression = "user") User actor) {
        List<Order> orders = (status == null || status.isBlank())
                ? naturalCustomerPort.listOrders(actor)
                : naturalCustomerPort.listOrdersByStatus(actor, OrderStatus.fromCode(status));
        return orders.stream()
                .map(orderRestMapper::toResponse)
                .toList();
    }

    @GetMapping("/orders/{id}")
    public OrderResponse getOrder(@PathVariable String id,
                                  @AuthenticationPrincipal(expression = "user") User actor) {
        return orderRestMapper.toResponse(naturalCustomerPort.getOrder(actor, id));
    }

    @PostMapping("/orders/{id}/pay")
    public OrderResponse payOrder(@PathVariable String id,
                                  @AuthenticationPrincipal(expression = "user") User actor) {
        return orderRestMapper.toResponse(naturalCustomerPort.payOrder(actor, id));
    }

    @PostMapping("/orders/{id}/cancel")
    public OrderResponse cancelOrder(@PathVariable String id,
                                     @AuthenticationPrincipal(expression = "user") User actor) {
        return orderRestMapper.toResponse(naturalCustomerPort.cancelOrder(actor, id));
    }

    @PostMapping("/orders/{id}/finalize")
    public OrderResponse finalizeOrder(@PathVariable String id,
                                       @AuthenticationPrincipal(expression = "user") User actor) {
        return orderRestMapper.toResponse(naturalCustomerPort.finalizeOrder(actor, id));
    }

    @GetMapping("/orders/{id}/invoice")
    public InvoiceResponse getInvoice(@PathVariable String id,
                                      @AuthenticationPrincipal(expression = "user") User actor) {
        Invoice invoice = naturalCustomerPort.getInvoice(actor, id);
        return invoiceRestMapper.toResponse(invoice);
    }

    @PostMapping("/orders/{id}/returns")
    public ResponseEntity<ReturnResponse> requestReturn(
            @PathVariable String id,
            @Valid @RequestBody RequestReturnRequest request,
            @AuthenticationPrincipal(expression = "user") User actor) {
        Return returnRequest = naturalCustomerPort.requestReturn(actor, id, request.reason());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(returnRestMapper.toResponse(returnRequest));
    }

    @GetMapping("/returns")
    public List<ReturnResponse> listReturns(@AuthenticationPrincipal(expression = "user") User actor) {
        return naturalCustomerPort.listReturns(actor).stream()
                .map(returnRestMapper::toResponse)
                .toList();
    }

    @GetMapping("/refunds")
    public List<RefundResponse> listRefunds(@AuthenticationPrincipal(expression = "user") User actor) {
        return naturalCustomerPort.listRefunds(actor).stream()
                .map(refundRestMapper::toResponse)
                .toList();
    }

    @GetMapping("/addresses")
    public List<AddressResponse> listAddresses(
            @AuthenticationPrincipal(expression = "user") User actor) {
        return buyerRestMapper.toAddressResponses(naturalCustomerPort.listAddresses(actor));
    }

    @PostMapping("/addresses")
    public BuyerResponse addAddress(@Valid @RequestBody AddAddressRequest request,
                                    @AuthenticationPrincipal(expression = "user") User actor) {
        Buyer buyer = naturalCustomerPort.addAddress(actor, toAddress(request.address()));
        return buyerRestMapper.toResponse(buyer);
    }

    @PutMapping("/addresses/main")
    public BuyerResponse changeMainAddress(
            @Valid @RequestBody ChangeMainAddressRequest request,
            @AuthenticationPrincipal(expression = "user") User actor) {
        Buyer buyer = naturalCustomerPort.changeMainAddress(actor, toAddress(request.address()));
        return buyerRestMapper.toResponse(buyer);
    }

    private Address toAddress(AddressRequest request) {
        if (request == null) {
            throw new InvalidDataException("Address is required");
        }
        return Address.of(
                request.street(),
                request.city(),
                request.region(),
                request.postalCode(),
                request.country());
    }
}
