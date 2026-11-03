package application.adapters.rest.controllers;

import application.adapters.rest.dto.response.AuditLogResponse;
import application.adapters.rest.dto.response.InventorySummaryResponse;
import application.adapters.rest.dto.response.OrderResponse;
import application.adapters.rest.dto.response.OrderSummaryResponse;
import application.adapters.rest.dto.response.ReturnSummaryResponse;
import application.adapters.rest.dto.response.SalesReportResponse;
import application.adapters.rest.dto.response.UserSummaryResponse;
import application.adapters.rest.mappers.AuditLogRestMapper;
import application.adapters.rest.mappers.OrderRestMapper;
import application.adapters.rest.mappers.ReportRestMapper;
import application.domain.enums.AuditAction;
import application.domain.exceptions.common.InvalidDataException;
import application.domain.models.User;
import application.domain.ports.in.BusinessSupervisorPort;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/supervisor")
@Validated
public class SupervisorController {

    private final BusinessSupervisorPort supervisorPort;
    private final ReportRestMapper reportRestMapper;
    private final OrderRestMapper orderRestMapper;
    private final AuditLogRestMapper auditLogRestMapper;

    public SupervisorController(BusinessSupervisorPort supervisorPort,
                                ReportRestMapper reportRestMapper,
                                OrderRestMapper orderRestMapper,
                                AuditLogRestMapper auditLogRestMapper) {
        this.supervisorPort = supervisorPort;
        this.reportRestMapper = reportRestMapper;
        this.orderRestMapper = orderRestMapper;
        this.auditLogRestMapper = auditLogRestMapper;
    }

    @GetMapping("/reports/sales")
    public SalesReportResponse salesReport(
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal User actor) {
        if (from == null || to == null) {
            throw new InvalidDataException("Both from and to are required");
        }
        return reportRestMapper.toResponse(supervisorPort.salesReport(actor, from, to));
    }

    @GetMapping("/reports/orders")
    public OrderSummaryResponse orderSummary(@AuthenticationPrincipal User actor) {
        return reportRestMapper.toResponse(supervisorPort.orderSummary(actor));
    }

    @GetMapping("/reports/inventory")
    public InventorySummaryResponse inventorySummary(@AuthenticationPrincipal User actor) {
        return reportRestMapper.toResponse(supervisorPort.inventorySummary(actor));
    }

    @GetMapping("/reports/returns")
    public ReturnSummaryResponse returnSummary(@AuthenticationPrincipal User actor) {
        return reportRestMapper.toResponse(supervisorPort.returnSummary(actor));
    }

    @GetMapping("/reports/users")
    public UserSummaryResponse userSummary(@AuthenticationPrincipal User actor) {
        return reportRestMapper.toResponse(supervisorPort.userSummary(actor));
    }

    @GetMapping("/orders")
    public List<OrderResponse> listOrders(@AuthenticationPrincipal User actor) {
        return supervisorPort.listOrders(actor).stream()
                .map(orderRestMapper::toResponse)
                .toList();
    }

    @GetMapping("/audit-logs")
    public List<AuditLogResponse> listAuditLogs(
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal User actor) {
        AuditAction parsedAction = (action == null || action.isBlank())
                ? null
                : AuditAction.fromCode(action);
        return supervisorPort.listAuditLogs(actor, parsedAction, from, to).stream()
                .map(auditLogRestMapper::toResponse)
                .toList();
    }
}
