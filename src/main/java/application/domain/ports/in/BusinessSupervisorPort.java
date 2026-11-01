package application.domain.ports.in;

import application.domain.enums.AuditAction;
import application.domain.models.AuditLog;
import application.domain.models.Order;
import application.domain.models.User;
import application.domain.models.reports.InventorySummary;
import application.domain.models.reports.OrderSummary;
import application.domain.models.reports.ReturnSummary;
import application.domain.models.reports.SalesReport;
import application.domain.models.reports.UserSummary;
import java.time.LocalDate;
import java.util.List;

public interface BusinessSupervisorPort {

    SalesReport salesReport(User actor, LocalDate from, LocalDate to);

    OrderSummary orderSummary(User actor);

    InventorySummary inventorySummary(User actor);

    ReturnSummary returnSummary(User actor);

    UserSummary userSummary(User actor);

    List<Order> listOrders(User actor);

    List<AuditLog> listAuditLogs(User actor, AuditAction action,
                                 LocalDate from, LocalDate to);
}
