package application.adapters.rest.controllers;

import application.adapters.rest.mappers.AuditLogRestMapper;
import application.adapters.rest.mappers.OrderRestMapper;
import application.adapters.rest.mappers.ReportRestMapper;
import application.domain.DomainFixtures;
import application.domain.enums.AuditAction;
import application.domain.enums.AuditEntityType;
import application.domain.enums.UserRole;
import application.domain.exceptions.common.InvalidDateRangeException;
import application.domain.exceptions.common.NoDataException;
import application.domain.models.AuditLog;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.models.User;
import application.domain.models.reports.InventorySummary;
import application.domain.models.reports.OrderSummary;
import application.domain.models.reports.ReturnSummary;
import application.domain.models.reports.SalesReport;
import application.domain.models.reports.UserSummary;
import application.domain.valueobjects.Money;
import application.infrastructure.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static application.adapters.rest.controllers.RestControllerTestSupport.actor;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SupervisorControllerTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private static final LocalDate FROM = LocalDate.parse("2026-01-01");
    private static final LocalDate TO = LocalDate.parse("2026-01-31");

    @Mock
    private application.domain.ports.in.BusinessSupervisorPort supervisorPort;

    private MockMvc mockMvc;
    private User supervisor;
    private Order order;
    private AuditLog auditLog;

    @BeforeEach
    void setUp() {
        supervisor = DomainFixtures.user(UserRole.SUPERVISOR);

        SupervisorController controller = new SupervisorController(
                supervisorPort,
                new ReportRestMapper(),
                new OrderRestMapper(),
                new AuditLogRestMapper());

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(RestControllerTestSupport.actorResolver())
                .build();

        order = paidOrder();
        auditLog = AuditLog.create(
                AuditAction.USER_CREATED,
                AuditEntityType.USER,
                supervisor.getId(),
                supervisor,
                NOW,
                "created");
    }

    @Test
    void salesReportReturns200() throws Exception {
        when(supervisorPort.salesReport(supervisor, FROM, TO)).thenReturn(new SalesReport(
                FROM, TO, 3, Money.cop(new BigDecimal("1500.00"))));

        mockMvc.perform(get("/api/v1/supervisor/reports/sales")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31")
                        .with(actor(supervisor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-01-01"))
                .andExpect(jsonPath("$.to").value("2026-01-31"))
                .andExpect(jsonPath("$.orderCount").value(3))
                .andExpect(jsonPath("$.grossRevenue.amount").value(1500.00))
                .andExpect(jsonPath("$.grossRevenue.currency").value("COP"))
                .andExpect(jsonPath("$.currency").value("COP"))
                .andExpect(jsonPath("$.generatedAt").exists());

        verify(supervisorPort).salesReport(supervisor, FROM, TO);
    }

    @Test
    void orderSummaryReturns200() throws Exception {
        when(supervisorPort.orderSummary(supervisor))
                .thenReturn(new OrderSummary(10, 2, 3, 1, 2, 1, 1));

        mockMvc.perform(get("/api/v1/supervisor/reports/orders").with(actor(supervisor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(10))
                .andExpect(jsonPath("$.ordersByStatus.PENDING_PAYMENT").value(2))
                .andExpect(jsonPath("$.ordersByStatus.PAID").value(3))
                .andExpect(jsonPath("$.ordersByStatus.DISPATCHED").value(1))
                .andExpect(jsonPath("$.ordersByStatus.DELIVERED").value(2))
                .andExpect(jsonPath("$.ordersByStatus.FINALIZED").value(1))
                .andExpect(jsonPath("$.ordersByStatus.CANCELLED").value(1))
                .andExpect(jsonPath("$.pendingPayment").value(2))
                .andExpect(jsonPath("$.paid").value(3));

        verify(supervisorPort).orderSummary(supervisor);
    }

    @Test
    void inventorySummaryReturns200() throws Exception {
        when(supervisorPort.inventorySummary(supervisor))
                .thenReturn(new InventorySummary(5, 40, 10, 2));

        mockMvc.perform(get("/api/v1/supervisor/reports/inventory").with(actor(supervisor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords").value(5))
                .andExpect(jsonPath("$.totalAvailable").value(40))
                .andExpect(jsonPath("$.totalReserved").value(10))
                .andExpect(jsonPath("$.damagedRecords").value(2))
                .andExpect(jsonPath("$.byWarehouse").isEmpty());

        verify(supervisorPort).inventorySummary(supervisor);
    }

    @Test
    void returnSummaryReturns200() throws Exception {
        when(supervisorPort.returnSummary(supervisor))
                .thenReturn(new ReturnSummary(6, 1, 2, 1, 2));

        mockMvc.perform(get("/api/v1/supervisor/reports/returns").with(actor(supervisor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReturns").value(6))
                .andExpect(jsonPath("$.returnsByStatus.REQUESTED").value(1))
                .andExpect(jsonPath("$.returnsByStatus.APPROVED").value(2))
                .andExpect(jsonPath("$.returnsByStatus.REJECTED").value(1))
                .andExpect(jsonPath("$.returnsByStatus.COMPLETED").value(2))
                .andExpect(jsonPath("$.approved").value(2))
                .andExpect(jsonPath("$.rejected").value(1))
                .andExpect(jsonPath("$.completed").value(2));

        verify(supervisorPort).returnSummary(supervisor);
    }

    @Test
    void userSummaryReturns200() throws Exception {
        when(supervisorPort.userSummary(supervisor)).thenReturn(new UserSummary(
                10, Map.of(UserRole.BUYER, 4, UserRole.SELLER, 6)));

        mockMvc.perform(get("/api/v1/supervisor/reports/users").with(actor(supervisor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(10))
                .andExpect(jsonPath("$.usersByRole.BUYER").value(4))
                .andExpect(jsonPath("$.usersByRole.SELLER").value(6))
                .andExpect(jsonPath("$.usersByRole.ADMIN").value(0))
                .andExpect(jsonPath("$.active").value(0))
                .andExpect(jsonPath("$.blocked").value(0))
                .andExpect(jsonPath("$.inactive").value(0));

        verify(supervisorPort).userSummary(supervisor);
    }

    @Test
    void listOrdersReturns200() throws Exception {
        when(supervisorPort.listOrders(supervisor)).thenReturn(List.of(order));

        mockMvc.perform(get("/api/v1/supervisor/orders").with(actor(supervisor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(order.getId()))
                .andExpect(jsonPath("$[0].buyerId").value(order.getBuyer().getId()))
                .andExpect(jsonPath("$[0].status").value("PAID"))
                .andExpect(jsonPath("$[0].paymentStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$[0].totalAmount.amount").value(238.00))
                .andExpect(jsonPath("$[0].totalAmount.currency").value("COP"))
                .andExpect(jsonPath("$[0].items.length()").value(1))
                .andExpect(jsonPath("$[0].items[0].productName").value("Laptop"))
                .andExpect(jsonPath("$[0].items[0].quantity").value(2))
                .andExpect(jsonPath("$[0].allocations").isEmpty())
                .andExpect(jsonPath("$[0].deliveryAddress.city").value("Bogota"))
                .andExpect(jsonPath("$[0].invoiceId").doesNotExist());

        verify(supervisorPort).listOrders(supervisor);
    }

    @Test
    void listAuditLogsReturns200() throws Exception {
        when(supervisorPort.listAuditLogs(supervisor, AuditAction.USER_CREATED, FROM, TO))
                .thenReturn(List.of(auditLog));

        mockMvc.perform(get("/api/v1/supervisor/audit-logs")
                        .param("action", "USER_CREATED")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31")
                        .with(actor(supervisor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(auditLog.getId()))
                .andExpect(jsonPath("$[0].action").value("USER_CREATED"))
                .andExpect(jsonPath("$[0].entityType").value("USER"))
                .andExpect(jsonPath("$[0].entityId").value(supervisor.getId()))
                .andExpect(jsonPath("$[0].performedBy").value(supervisor.getId()))
                .andExpect(jsonPath("$[0].role").value("SUPERVISOR"))
                .andExpect(jsonPath("$[0].occurredAt").exists())
                .andExpect(jsonPath("$[0].details").value("created"));

        verify(supervisorPort).listAuditLogs(supervisor, AuditAction.USER_CREATED, FROM, TO);
    }

    @Test
    void blankAuditActionIsTreatedAsNull() throws Exception {
        when(supervisorPort.listAuditLogs(supervisor, null, null, null))
                .thenReturn(List.of(auditLog));

        mockMvc.perform(get("/api/v1/supervisor/audit-logs")
                        .param("action", "   ")
                        .with(actor(supervisor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(supervisorPort).listAuditLogs(supervisor, null, null, null);
    }

    @Test
    void invalidAuditActionReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/supervisor/audit-logs")
                        .param("action", "NOT_A_REAL_ACTION")
                        .with(actor(supervisor)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_DATA"))
                .andExpect(jsonPath("$.path").value("/api/v1/supervisor/audit-logs"));

        verifyNoInteractions(supervisorPort);
    }

    @Test
    void missingSalesDateParametersReturn400() throws Exception {
        mockMvc.perform(get("/api/v1/supervisor/reports/sales").with(actor(supervisor)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_DATA"))
                .andExpect(jsonPath("$.path").value("/api/v1/supervisor/reports/sales"));

        verifyNoInteractions(supervisorPort);
    }

    @Test
    void invalidDateFormatReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/supervisor/reports/sales")
                        .param("from", "not-a-date")
                        .param("to", "2026-01-31")
                        .with(actor(supervisor)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'from'"));

        verifyNoInteractions(supervisorPort);
    }

    @Test
    void invalidDateRangeReturns400() throws Exception {
        when(supervisorPort.salesReport(supervisor, FROM, TO))
                .thenThrow(new InvalidDateRangeException("Date range is invalid"));

        mockMvc.perform(get("/api/v1/supervisor/reports/sales")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31")
                        .with(actor(supervisor)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"))
                .andExpect(jsonPath("$.message").value("Date range is invalid"));
    }

    @Test
    void missingReportDataReturns404() throws Exception {
        when(supervisorPort.orderSummary(supervisor))
                .thenThrow(new NoDataException("No orders available"));

        mockMvc.perform(get("/api/v1/supervisor/reports/orders").with(actor(supervisor)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NO_DATA"))
                .andExpect(jsonPath("$.message").value("No orders available"));
    }

    private static Order paidOrder() {
        Seller seller = DomainFixtures.seller(DomainFixtures.admin());
        Product product = DomainFixtures.publishedPhysicalProduct(seller, DomainFixtures.operator());
        Order paid = Order.fromCart(
                DomainFixtures.cartWith(DomainFixtures.buyer(), product, 2),
                DomainFixtures.address(),
                NOW);
        paid.confirmPayment(NOW);
        return paid;
    }
}
