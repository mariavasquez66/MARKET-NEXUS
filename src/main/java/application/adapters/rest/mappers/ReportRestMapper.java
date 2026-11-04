package application.adapters.rest.mappers;

import application.adapters.rest.dto.response.InventorySummaryResponse;
import application.adapters.rest.dto.response.OrderSummaryResponse;
import application.adapters.rest.dto.response.ReturnSummaryResponse;
import application.adapters.rest.dto.response.SalesReportResponse;
import application.adapters.rest.dto.response.UserSummaryResponse;
import application.domain.enums.OrderStatus;
import application.domain.enums.ReturnStatus;
import application.domain.enums.UserRole;
import application.domain.models.reports.InventorySummary;
import application.domain.models.reports.OrderSummary;
import application.domain.models.reports.ReturnSummary;
import application.domain.models.reports.SalesReport;
import application.domain.models.reports.UserSummary;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ReportRestMapper extends RestMapperSupport {

    public SalesReportResponse toResponse(SalesReport report) {
        return new SalesReportResponse(
                report.from(),
                report.to(),
                report.orderCount(),
                toMoneyResponse(report.totalSales()),
                report.totalSales().getCurrency().getCurrencyCode(),
                Instant.now());
    }

    public OrderSummaryResponse toResponse(OrderSummary summary) {
        Map<String, Integer> byStatus = new LinkedHashMap<>();
        byStatus.put(OrderStatus.PENDING_PAYMENT.getCode(), summary.pendingPayment());
        byStatus.put(OrderStatus.PAID.getCode(), summary.paid());
        byStatus.put(OrderStatus.DISPATCHED.getCode(), summary.dispatched());
        byStatus.put(OrderStatus.DELIVERED.getCode(), summary.delivered());
        byStatus.put(OrderStatus.FINALIZED.getCode(), summary.finalized());
        byStatus.put(OrderStatus.CANCELLED.getCode(), summary.cancelled());
        return new OrderSummaryResponse(
                summary.totalOrders(),
                Map.copyOf(byStatus),
                summary.pendingPayment(),
                summary.paid(),
                summary.dispatched(),
                summary.delivered(),
                summary.finalized(),
                summary.cancelled());
    }

    public InventorySummaryResponse toResponse(InventorySummary summary) {
        return new InventorySummaryResponse(
                summary.totalRecords(),
                summary.totalAvailable(),
                summary.totalReserved(),
                summary.damagedRecords(),
                Map.of());
    }

    public ReturnSummaryResponse toResponse(ReturnSummary summary) {
        Map<String, Integer> byStatus = new LinkedHashMap<>();
        byStatus.put(ReturnStatus.REQUESTED.getCode(), summary.requested());
        byStatus.put(ReturnStatus.APPROVED.getCode(), summary.approved());
        byStatus.put(ReturnStatus.REJECTED.getCode(), summary.rejected());
        byStatus.put(ReturnStatus.COMPLETED.getCode(), summary.completed());
        return new ReturnSummaryResponse(
                summary.totalReturns(),
                Map.copyOf(byStatus),
                summary.approved(),
                summary.rejected(),
                summary.completed());
    }

    public UserSummaryResponse toResponse(UserSummary summary) {
        Map<String, Integer> byRole = new LinkedHashMap<>();
        for (UserRole role : UserRole.values()) {
            byRole.put(role.getCode(), summary.usersByRole().getOrDefault(role, 0));
        }
        return new UserSummaryResponse(
                summary.totalUsers(),
                Map.copyOf(byRole),
                0,
                0,
                0);
    }
}
