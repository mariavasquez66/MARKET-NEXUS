package application.infrastructure.config;

import application.domain.ports.out.AuditPort;
import application.domain.ports.out.BuyerRepositoryPort;
import application.domain.ports.out.CartRepositoryPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.InventoryMovementRepositoryPort;
import application.domain.ports.out.InventoryRepositoryPort;
import application.domain.ports.out.InvoiceRepositoryPort;
import application.domain.ports.out.JwtServicePort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.PasswordServicePort;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.ports.out.RefundRepositoryPort;
import application.domain.ports.out.ReturnRepositoryPort;
import application.domain.ports.out.SellerRepositoryPort;
import application.domain.ports.out.ShipmentRepositoryPort;
import application.domain.ports.out.TaxPolicyPort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.ports.out.WarehouseRepositoryPort;
import application.domain.services.AuthService;
import application.domain.services.BuyerService;
import application.domain.services.CartService;
import application.domain.services.CatalogService;
import application.domain.services.InventoryService;
import application.domain.services.InvoiceService;
import application.domain.services.LogisticsService;
import application.domain.services.OrderService;
import application.domain.services.RefundService;
import application.domain.services.ReportService;
import application.domain.services.ReturnService;
import application.domain.services.SellerService;
import application.domain.services.UserService;
import application.domain.services.WarehouseService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServicesConfig {

    @Bean
    public AuthService authService(UserRepositoryPort userRepository,
                                   BuyerRepositoryPort buyerRepository,
                                   PasswordServicePort passwordService,
                                   JwtServicePort jwtService,
                                   AuditPort auditPort,
                                   ClockPort clockPort) {
        return new AuthService(userRepository, buyerRepository, passwordService,
                jwtService, auditPort, clockPort);
    }

    @Bean
    public BuyerService buyerService(BuyerRepositoryPort buyerRepository,
                                     AuditPort auditPort,
                                     ClockPort clockPort) {
        return new BuyerService(buyerRepository, auditPort, clockPort);
    }

    @Bean
    public CartService cartService(CartRepositoryPort cartRepository,
                                   BuyerRepositoryPort buyerRepository,
                                   ProductRepositoryPort productRepository) {
        return new CartService(cartRepository, buyerRepository, productRepository);
    }

    @Bean
    public CatalogService catalogService(ProductRepositoryPort productRepository,
                                         SellerRepositoryPort sellerRepository,
                                         InventoryRepositoryPort inventoryRepository,
                                         AuditPort auditPort,
                                         ClockPort clockPort) {
        return new CatalogService(productRepository, sellerRepository, inventoryRepository,
                auditPort, clockPort);
    }

    @Bean
    public InventoryService inventoryService(InventoryRepositoryPort inventoryRepository,
                                             InventoryMovementRepositoryPort movementRepository,
                                             ProductRepositoryPort productRepository,
                                             WarehouseRepositoryPort warehouseRepository,
                                             AuditPort auditPort,
                                             ClockPort clockPort) {
        return new InventoryService(inventoryRepository, movementRepository, productRepository,
                warehouseRepository, auditPort, clockPort);
    }

    @Bean
    public InvoiceService invoiceService(InvoiceRepositoryPort invoiceRepository,
                                         OrderRepositoryPort orderRepository,
                                         TaxPolicyPort taxPolicy,
                                         AuditPort auditPort,
                                         ClockPort clockPort) {
        return new InvoiceService(invoiceRepository, orderRepository, taxPolicy, auditPort, clockPort);
    }

    @Bean
    public OrderService orderService(CartRepositoryPort cartRepository,
                                     BuyerRepositoryPort buyerRepository,
                                     OrderRepositoryPort orderRepository,
                                     ProductRepositoryPort productRepository,
                                     InventoryRepositoryPort inventoryRepository,
                                     InventoryMovementRepositoryPort movementRepository,
                                     ShipmentRepositoryPort shipmentRepository,
                                     InvoiceService invoiceService,
                                     AuditPort auditPort,
                                     ClockPort clockPort) {
        return new OrderService(cartRepository, buyerRepository, orderRepository, productRepository,
                inventoryRepository, movementRepository, shipmentRepository, invoiceService,
                auditPort, clockPort);
    }

    @Bean
    public LogisticsService logisticsService(ShipmentRepositoryPort shipmentRepository,
                                             OrderRepositoryPort orderRepository,
                                             WarehouseRepositoryPort warehouseRepository,
                                             OrderService orderService,
                                             AuditPort auditPort,
                                             ClockPort clockPort) {
        return new LogisticsService(shipmentRepository, orderRepository, warehouseRepository,
                orderService, auditPort, clockPort);
    }

    @Bean
    public RefundService refundService(RefundRepositoryPort refundRepository,
                                       ReturnRepositoryPort returnRepository,
                                       OrderRepositoryPort orderRepository,
                                       AuditPort auditPort,
                                       ClockPort clockPort) {
        return new RefundService(refundRepository, returnRepository, orderRepository,
                auditPort, clockPort);
    }

    @Bean
    public ReportService reportService(OrderRepositoryPort orderRepository,
                                       InventoryRepositoryPort inventoryRepository,
                                       ReturnRepositoryPort returnRepository,
                                       UserRepositoryPort userRepository,
                                       AuditPort auditPort,
                                       ClockPort clockPort) {
        return new ReportService(orderRepository, inventoryRepository, returnRepository,
                userRepository, auditPort, clockPort);
    }

    @Bean
    public ReturnService returnService(ReturnRepositoryPort returnRepository,
                                       OrderRepositoryPort orderRepository,
                                       InventoryService inventoryService,
                                       AuditPort auditPort,
                                       ClockPort clockPort) {
        return new ReturnService(returnRepository, orderRepository, inventoryService,
                auditPort, clockPort);
    }

    @Bean
    public SellerService sellerService(SellerRepositoryPort sellerRepository,
                                       UserRepositoryPort userRepository,
                                       WarehouseRepositoryPort warehouseRepository,
                                       AuditPort auditPort,
                                       ClockPort clockPort) {
        return new SellerService(sellerRepository, userRepository, warehouseRepository,
                auditPort, clockPort);
    }

    @Bean
    public UserService userService(UserRepositoryPort userRepository,
                                   PasswordServicePort passwordService,
                                   AuditPort auditPort,
                                   ClockPort clockPort) {
        return new UserService(userRepository, passwordService, auditPort, clockPort);
    }

    @Bean
    public WarehouseService warehouseService(WarehouseRepositoryPort warehouseRepository,
                                             SellerRepositoryPort sellerRepository,
                                             UserRepositoryPort userRepository,
                                             AuditPort auditPort,
                                             ClockPort clockPort) {
        return new WarehouseService(warehouseRepository, sellerRepository, userRepository,
                auditPort, clockPort);
    }
}
