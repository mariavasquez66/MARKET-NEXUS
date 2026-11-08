package application.domain.services;

import static application.domain.DomainFixtures.address;
import static application.domain.DomainFixtures.admin;
import static application.domain.DomainFixtures.buyer;
import static application.domain.DomainFixtures.cartWith;
import static application.domain.DomainFixtures.operator;
import static application.domain.DomainFixtures.publishedPhysicalProduct;
import static application.domain.DomainFixtures.seller;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import application.domain.enums.PaymentStatus;
import application.domain.enums.RefundStatus;
import application.domain.exceptions.common.InsufficientPermissionException;
import application.domain.exceptions.refund.InvalidRefundAmountException;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.Refund;
import application.domain.models.Return;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.RefundRepositoryPort;
import application.domain.ports.out.ReturnRepositoryPort;
import application.domain.valueobjects.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RefundServiceTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock RefundRepositoryPort refundRepository;
    @Mock ReturnRepositoryPort returnRepository;
    @Mock OrderRepositoryPort orderRepository;
    @Mock AuditPort auditPort;
    @Mock ClockPort clockPort;

    private RefundService service;

    @BeforeEach
    void setUp() {
        service = new RefundService(refundRepository, returnRepository, orderRepository,
                auditPort, clockPort);
        when(clockPort.now()).thenReturn(NOW);
        when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void onlyAdminCanGenerateRefund() {
        Return returnRequest = approvedReturn();
        assertThatThrownBy(() -> service.generateRefund(returnRequest.getOrder().getBuyer().getUser(),
                returnRequest.getId(), Money.cop(BigDecimal.TEN)))
                .isInstanceOf(InsufficientPermissionException.class);
    }

    @Test
    void accumulatedRefundsCannotExceedOrderTotal() {
        Return returnRequest = approvedReturn();
        when(returnRepository.findById(returnRequest.getId())).thenReturn(Optional.of(returnRequest));
        when(refundRepository.findByOrderId(returnRequest.getOrder().getId()))
                .thenReturn(List.of(Refund.create(returnRequest,
                        Money.cop(new BigDecimal("100.00")),
                        returnRequest.getOrder().getTotalAmount())));
        assertThatThrownBy(() -> service.generateRefund(admin(), returnRequest.getId(),
                Money.cop(new BigDecimal("20.00"))))
                .isInstanceOf(InvalidRefundAmountException.class);
    }

    @Test
    void accumulatedRefundsAcrossReturnsCannotExceedOrderTotal() {
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Order order = Order.fromCart(cartWith(buyer(), product, 1), address(), NOW);
        order.confirmPayment(NOW);
        order.markDispatched();
        order.markDelivered();
        Return first = Return.request(order, "Broken", NOW);
        first.approve();
        Return second = Return.request(order, "Wrong item", NOW);
        second.approve();
        when(returnRepository.findById(second.getId())).thenReturn(Optional.of(second));
        when(refundRepository.findByOrderId(order.getId()))
                .thenReturn(List.of(Refund.create(first, Money.cop(new BigDecimal("100.00")),
                        order.getTotalAmount())));

        assertThatThrownBy(() -> service.generateRefund(admin(), second.getId(),
                Money.cop(new BigDecimal("20.00"))))
                .isInstanceOf(InvalidRefundAmountException.class);
    }

    @Test
    void processingRefundDoesNotChangeOrderPaymentStatus() {
        Return returnRequest = approvedReturn();
        Order order = returnRequest.getOrder();
        Instant paidAt = order.getPaidAt();
        Refund refund = Refund.create(returnRequest, Money.cop(new BigDecimal("10.00")),
                order.getTotalAmount());
        when(refundRepository.findById(refund.getId())).thenReturn(Optional.of(refund));

        service.process(admin(), refund.getId());

        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(order.getPaidAt()).isEqualTo(paidAt);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void refundIsCreatedAndProcessedOnce() {
        Return returnRequest = approvedReturn();
        when(returnRepository.findById(returnRequest.getId())).thenReturn(Optional.of(returnRequest));
        when(refundRepository.findByOrderId(returnRequest.getOrder().getId())).thenReturn(List.of());
        Refund refund = service.generateRefund(admin(), returnRequest.getId(),
                Money.cop(new BigDecimal("10.00")));
        when(refundRepository.findById(refund.getId())).thenReturn(Optional.of(refund));
        Refund processed = service.process(admin(), refund.getId());
        assertThat(processed.getStatus()).isEqualTo(RefundStatus.PROCESSED);
        verify(auditPort, org.mockito.Mockito.times(2)).record(any());
    }

    private Return approvedReturn() {
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Order order = Order.fromCart(cartWith(buyer(), product, 1), address(), NOW);
        order.confirmPayment(NOW);
        order.markDispatched();
        order.markDelivered();
        Return returnRequest = Return.request(order, "Broken", NOW);
        returnRequest.approve();
        return returnRequest;
    }
}
