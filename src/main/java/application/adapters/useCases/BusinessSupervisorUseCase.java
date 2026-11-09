package application.adapters.useCases;

import application.domain.enums.AuditAction;
import application.domain.models.AuditLog;
import application.domain.models.Order;
import application.domain.models.User;
import application.domain.models.reports.InventorySummary;
import application.domain.models.reports.OrderSummary;
import application.domain.models.reports.ReturnSummary;
import application.domain.models.reports.SalesReport;
import application.domain.models.reports.UserSummary;
import application.domain.ports.in.BusinessSupervisorPort;
import application.domain.services.OrderService;
import application.domain.services.ReportService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class BusinessSupervisorUseCase implements BusinessSupervisorPort {

    private final ReportService reportService;
    private final OrderService orderService;

    public BusinessSupervisorUseCase(ReportService reportService, OrderService orderService) {
        this.reportService = reportService;
        this.orderService = orderService;
    }

    @Override
    public SalesReport salesReport(User actor, LocalDate from, LocalDate to) {
        return reportService.salesReport(actor, from, to);
    }

    @Override
    public OrderSummary orderSummary(User actor) {
        return reportService.orderSummary(actor);
    }

    @Override
    public InventorySummary inventorySummary(User actor) {
        return reportService.inventorySummary(actor);
    }

    @Override
    public ReturnSummary returnSummary(User actor) {
        return reportService.returnSummary(actor);
    }

    @Override
    public UserSummary userSummary(User actor) {
        return reportService.userSummary(actor);
    }

    @Override
    public List<Order> listOrders(User actor) {
        return orderService.listOrders(actor);
    }

    @Override
    public List<AuditLog> listAuditLogs(User actor, AuditAction action,
                                        LocalDate from, LocalDate to) {
        return reportService.auditLogs(actor, action, from, to);
    }
}
