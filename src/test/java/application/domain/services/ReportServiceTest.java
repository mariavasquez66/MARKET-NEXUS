package application.domain.services;

import static application.domain.DomainFixtures.address;
import static application.domain.DomainFixtures.admin;
import static application.domain.DomainFixtures.buyer;
import static application.domain.DomainFixtures.cartWith;
import static application.domain.DomainFixtures.operator;
import static application.domain.DomainFixtures.publishedPhysicalProduct;
import static application.domain.DomainFixtures.seller;
import static application.domain.DomainFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import application.domain.enums.PaymentStatus;
import application.domain.enums.ReturnStatus;
import application.domain.enums.UserRole;
import application.domain.exceptions.common.InvalidDateRangeException;
import application.domain.exceptions.common.NoDataException;
import application.domain.models.Inventory;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.Return;
import application.domain.models.reports.OrderSummary;
import application.domain.models.reports.SalesReport;
import application.domain.models.reports.UserSummary;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.InventoryRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.ReturnRepositoryPort;
import application.domain.ports.out.UserRepositoryPort;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportServiceTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock OrderRepositoryPort orderRepository;
    @Mock InventoryRepositoryPort inventoryRepository;
    @Mock ReturnRepositoryPort returnRepository;
    @Mock UserRepositoryPort userRepository;
    @Mock AuditPort auditPort;
    @Mock ClockPort clockPort;

    private ReportService service;

    @BeforeEach
    void setUp() {
        service = new ReportService(orderRepository, inventoryRepository, returnRepository,
                userRepository, auditPort, clockPort);
        when(clockPort.today()).thenReturn(LocalDate.of(2026, 1, 1));
    }

    @Test
    void onlySupervisorCanRunReports() {
        assertThatThrownBy(() -> service.orderSummary(admin()))
                .isInstanceOf(application.domain.exceptions.common.InsufficientPermissionException.class);
    }

    @Test
    void invertedDateRangeIsRejected() {
        assertThatThrownBy(() -> service.salesReport(supervisor(),
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(InvalidDateRangeException.class);
    }

    @Test
    void emptySalesReportIsNotFound() {
        when(orderRepository.findByPaymentStatusAndCreatedAtBetween(
                org.mockito.ArgumentMatchers.eq(PaymentStatus.CONFIRMED),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
        assertThatThrownBy(() -> service.salesReport(supervisor(),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2)))
                .isInstanceOf(NoDataException.class);
    }

    @Test
    void salesReportAggregatesConfirmedOrders() {
        Order order = paidOrder();
        when(orderRepository.findByPaymentStatusAndCreatedAtBetween(
                org.mockito.ArgumentMatchers.eq(PaymentStatus.CONFIRMED),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(order));
        SalesReport report = service.salesReport(supervisor(),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2));
        assertThat(report.orderCount()).isEqualTo(1);
        assertThat(report.totalSales().getAmount()).isEqualByComparingTo("119.00");
    }

    @Test
    void orderSummaryCountsStatuses() {
        when(orderRepository.findAll()).thenReturn(List.of(paidOrder()));
        OrderSummary summary = service.orderSummary(supervisor());
        assertThat(summary.totalOrders()).isEqualTo(1);
        assertThat(summary.paid()).isEqualTo(1);
    }

    @Test
    void inventorySummaryAggregatesQuantities() {
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Inventory inventory = Inventory.create(product,
                product.getSeller().getWarehouses().get(0), 5);
        inventory.reserve(2);
        when(inventoryRepository.findAll()).thenReturn(List.of(inventory));
        var summary = service.inventorySummary(supervisor());
        assertThat(summary.totalAvailable()).isEqualTo(3);
        assertThat(summary.totalReserved()).isEqualTo(2);
    }

    @Test
    void returnSummaryCountsStatuses() {
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Order order = Order.fromCart(cartWith(buyer(), product, 1), address(), NOW);
        order.confirmPayment(NOW);
        order.markDispatched();
        order.markDelivered();
        Return returnRequest = Return.request(order, "Broken", NOW);
        when(returnRepository.findAll()).thenReturn(List.of(returnRequest));
        var summary = service.returnSummary(supervisor());
        assertThat(summary.totalReturns()).isEqualTo(1);
        assertThat(summary.requested()).isEqualTo(1);
        assertThat(returnRequest.getStatus()).isEqualTo(ReturnStatus.REQUESTED);
    }

    @Test
    void userSummaryCountsRoles() {
        when(userRepository.findAll()).thenReturn(List.of(user(UserRole.BUYER), user(UserRole.BUYER),
                user(UserRole.SELLER)));
        UserSummary summary = service.userSummary(supervisor());
        assertThat(summary.totalUsers()).isEqualTo(3);
        assertThat(summary.usersByRole().get(UserRole.BUYER)).isEqualTo(2);
    }

    private application.domain.models.User supervisor() {
        return user(UserRole.SUPERVISOR);
    }

    private Order paidOrder() {
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Order order = Order.fromCart(cartWith(buyer(), product, 1), address(), NOW);
        order.confirmPayment(NOW);
        return order;
    }
}
