package application.domain.services;

import application.domain.enums.AuditAction;
import application.domain.enums.AuditEntityType;
import application.domain.enums.OrderStatus;
import application.domain.enums.UserRole;
import application.domain.exceptions.common.UnauthorizedOperationException;
import application.domain.exceptions.invoice.InvoiceAlreadyExistsException;
import application.domain.exceptions.invoice.InvoiceNotFoundException;
import application.domain.exceptions.order.InvalidStateTransitionException;
import application.domain.exceptions.order.OrderNotFoundException;
import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.User;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.InvoiceRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.TaxPolicyPort;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class InvoiceService {
    private final InvoiceRepositoryPort invoiceRepository;
    private final OrderRepositoryPort orderRepository;
    private final TaxPolicyPort taxPolicy;
    private final AuditPort auditPort;
    private final ClockPort clockPort;

    public InvoiceService(InvoiceRepositoryPort invoiceRepository,
                          OrderRepositoryPort orderRepository,
                          TaxPolicyPort taxPolicy,
                          AuditPort auditPort,
                          ClockPort clockPort) {
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository);
        this.orderRepository = Objects.requireNonNull(orderRepository);
        this.taxPolicy = Objects.requireNonNull(taxPolicy);
        this.auditPort = Objects.requireNonNull(auditPort);
        this.clockPort = Objects.requireNonNull(clockPort);
    }

    public Invoice generateInvoice(User actor, String orderId) {
        ServiceSupport.requireAnyRole(actor, UserRole.BUYER, UserRole.ADMIN);
        Order order = requireOrder(orderId);
        if (!order.getStatus().equals(OrderStatus.PAID)) {
            throw new InvalidStateTransitionException("Invoice requires a paid order");
        }
        if (invoiceRepository.existsByOrderId(orderId)) {
            throw new InvoiceAlreadyExistsException("Order already has an invoice");
        }
        BigDecimal taxRate = taxPolicy.currentTaxRate();
        Invoice invoice = Invoice.create(order, "INV-" + orderId, taxRate, clockPort.now());
        Invoice saved = invoiceRepository.save(invoice);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.INVOICE_ISSUED,
                AuditEntityType.INVOICE, saved.getId(), actor, "Invoice issued");
        return saved;
    }

    public Invoice getInvoice(User actor, String invoiceId) {
        ServiceSupport.requireAnyRole(actor, UserRole.BUYER, UserRole.ADMIN, UserRole.SUPERVISOR);
        Invoice invoice = requireInvoice(invoiceId);
        requireVisibility(actor, invoice);
        return invoice;
    }

    public Invoice getByOrder(User actor, String orderId) {
        ServiceSupport.requireAnyRole(actor, UserRole.BUYER, UserRole.ADMIN, UserRole.SUPERVISOR);
        Invoice invoice = invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found for order: " + orderId));
        requireVisibility(actor, invoice);
        return invoice;
    }

    public List<Invoice> listByBuyer(User actor, String buyerId) {
        ServiceSupport.requireAnyRole(actor, UserRole.BUYER, UserRole.ADMIN, UserRole.SUPERVISOR);
        if (actor.hasRole(UserRole.BUYER)) {
            List<Invoice> invoices = invoiceRepository.findByBuyerId(buyerId);
            if (invoices.stream().anyMatch(invoice ->
                    !invoice.getOrder().getBuyer().getUser().getId().equals(actor.getId()))) {
                throw new UnauthorizedOperationException("Cannot access another buyer invoices");
            }
            return invoices;
        }
        return invoiceRepository.findByBuyerId(buyerId);
    }

    public Invoice voidInvoice(User actor, String invoiceId) {
        ServiceSupport.requireRole(actor, UserRole.ADMIN);
        Invoice invoice = requireInvoice(invoiceId);
        invoice.voidInvoice();
        Invoice saved = invoiceRepository.save(invoice);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.INVOICE_VOIDED,
                AuditEntityType.INVOICE, saved.getId(), actor, "Invoice voided");
        return saved;
    }

    private void requireVisibility(User actor, Invoice invoice) {
        if (actor.hasRole(UserRole.ADMIN) || actor.hasRole(UserRole.SUPERVISOR)) {
            return;
        }
        if (!invoice.getOrder().getBuyer().getUser().getId().equals(actor.getId())) {
            throw new UnauthorizedOperationException("Cannot access another buyer invoice");
        }
    }

    private Invoice requireInvoice(String invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found: " + invoiceId));
    }

    private Order requireOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }
}
