package application.adapters.rest.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import application.adapters.rest.mappers.RefundRestMapper;
import application.adapters.rest.mappers.ReturnRestMapper;
import application.adapters.rest.mappers.SellerRestMapper;
import application.adapters.rest.mappers.UserRestMapper;
import application.adapters.rest.mappers.WarehouseRestMapper;
import application.domain.DomainFixtures;
import application.domain.enums.OrderStatus;
import application.domain.enums.PaymentStatus;
import application.domain.enums.RefundStatus;
import application.domain.enums.ReturnStatus;
import application.domain.enums.UserRole;
import application.domain.enums.WarehouseType;
import application.domain.exceptions.common.DuplicateEntityException;
import application.domain.exceptions.seller.SelfRegistrationNotAllowedException;
import application.domain.exceptions.seller.SellerNotFoundException;
import application.domain.exceptions.user.InvalidRoleException;
import application.domain.models.Order;
import application.domain.models.Refund;
import application.domain.models.Return;
import application.domain.models.Seller;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.ports.in.TellerEmployeePort;
import application.domain.valueobjects.Address;
import application.domain.valueobjects.Email;
import application.domain.valueobjects.FullName;
import application.domain.valueobjects.IdentityDocument;
import application.domain.valueobjects.Money;
import application.domain.valueobjects.TaxId;
import application.infrastructure.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    private static final String SELLER_JSON =
            "{\"userId\":\"user-1\",\"taxId\":\"900123456-7\"}";
    private static final String UPDATE_SELLER_JSON =
            "{\"taxId\":\"900987654-3\"}";
    private static final String LOCATION_JSON =
            "\"location\":{\"street\":\"Calle 1 # 2-3\",\"city\":\"Bogota\","
                    + "\"region\":\"Cundinamarca\",\"postalCode\":\"110111\","
                    + "\"country\":\"Colombia\"}";
    private static final String CREATE_WAREHOUSE_JSON =
            "{\"name\":\"Central warehouse\"," + LOCATION_JSON
                    + ",\"type\":\"SELLER\",\"ownerSellerId\":\"seller-owner\","
                    + "\"responsibleUserId\":\"responsible-1\"}";
    private static final String UPDATE_WAREHOUSE_JSON =
            "{\"name\":\"Updated warehouse\"," + LOCATION_JSON
                    + ",\"responsibleUserId\":\"responsible-2\"}";
    private static final String CREATE_USER_JSON =
            "{\"fullName\":\"Jane Doe\",\"email\":\"jane@example.com\","
                    + "\"identityDocument\":\"DOC12345\",\"role\":\"SUPERVISOR\","
                    + "\"password\":\"secret123\"}";
    private static final Instant REQUESTED_AT = Instant.parse("2026-01-02T10:00:00Z");

    @Mock
    private TellerEmployeePort tellerEmployeePort;

    private MockMvc mockMvc;
    private User admin;
    private Seller seller;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        admin = DomainFixtures.admin();
        seller = DomainFixtures.seller(admin);
        warehouse = DomainFixtures.sellerWarehouse(seller, DomainFixtures.operator());
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminController(
                        tellerEmployeePort,
                        new SellerRestMapper(),
                        new WarehouseRestMapper(),
                        new UserRestMapper(),
                        new ReturnRestMapper(),
                        new RefundRestMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(RestControllerTestSupport.actorResolver())
                .build();
    }

    @Test
    void registerSellerReturnsCreated() throws Exception {
        when(tellerEmployeePort.registerSeller(
                admin, "user-1", TaxId.of("900123456-7"))).thenReturn(seller);

        mockMvc.perform(post("/api/v1/admin/sellers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SELLER_JSON)
                        .with(RestControllerTestSupport.actor(admin)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(seller.getId()))
                .andExpect(jsonPath("$.userId").value(seller.getUser().getId()))
                .andExpect(jsonPath("$.fullName").value(seller.getUser().getFullName().getValue()))
                .andExpect(jsonPath("$.email").value(seller.getUser().getEmail().getValue()))
                .andExpect(jsonPath("$.taxId").value("900123456-7"));
    }

    @Test
    void listSellersReturnsOk() throws Exception {
        when(tellerEmployeePort.listSellers(admin)).thenReturn(List.of(seller));

        mockMvc.perform(get("/api/v1/admin/sellers")
                        .with(RestControllerTestSupport.actor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(seller.getId()))
                .andExpect(jsonPath("$[0].warehouseIds[0]").value(warehouse.getId()));
    }

    @Test
    void updateSellerReturnsOk() throws Exception {
        Seller updated = Seller.reconstitute(seller.getId(), seller.getUser(),
                TaxId.of("900987654-3"), seller.getWarehouses());
        when(tellerEmployeePort.updateSellerTaxId(
                admin, seller.getId(), TaxId.of("900987654-3"))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/admin/sellers/{id}", seller.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_SELLER_JSON)
                        .with(RestControllerTestSupport.actor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(seller.getId()))
                .andExpect(jsonPath("$.taxId").value("900987654-3"));
    }

    @Test
    void createWarehouseReturnsCreated() throws Exception {
        when(tellerEmployeePort.createWarehouse(eq(admin), anyString(),
                any(Address.class), eq(WarehouseType.SELLER),
                eq("seller-owner"), eq("responsible-1"))).thenReturn(warehouse);

        mockMvc.perform(post("/api/v1/admin/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_WAREHOUSE_JSON)
                        .with(RestControllerTestSupport.actor(admin)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(warehouse.getId()))
                .andExpect(jsonPath("$.type").value("SELLER"))
                .andExpect(jsonPath("$.location.city").value("Bogota"))
                .andExpect(jsonPath("$.ownerSellerId").value(seller.getId()))
                .andExpect(jsonPath("$.responsibleUserId")
                        .value(warehouse.getResponsible().getId()));
    }

    @Test
    void listWarehousesReturnsOk() throws Exception {
        when(tellerEmployeePort.listWarehouses(admin)).thenReturn(List.of(warehouse));

        mockMvc.perform(get("/api/v1/admin/warehouses")
                        .with(RestControllerTestSupport.actor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(warehouse.getId()))
                .andExpect(jsonPath("$[0].name").value(warehouse.getName()))
                .andExpect(jsonPath("$[0].location.country").value("Colombia"));
    }

    @Test
    void updateWarehouseReturnsOk() throws Exception {
        User responsible = warehouse.getResponsible();
        Warehouse updated = Warehouse.reconstitute(warehouse.getId(), "Updated warehouse",
                DomainFixtures.address(), WarehouseType.SELLER, seller, responsible);
        when(tellerEmployeePort.updateWarehouse(eq(admin), eq(warehouse.getId()),
                eq("Updated warehouse"), any(Address.class),
                eq("responsible-2"))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/admin/warehouses/{id}", warehouse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_WAREHOUSE_JSON)
                        .with(RestControllerTestSupport.actor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(warehouse.getId()))
                .andExpect(jsonPath("$.name").value("Updated warehouse"));
    }

    @Test
    void assignWarehouseReturnsOk() throws Exception {
        when(tellerEmployeePort.assignWarehouse(
                admin, seller.getId(), warehouse.getId())).thenReturn(seller);

        mockMvc.perform(post("/api/v1/admin/sellers/{id}/warehouses/{warehouseId}",
                        seller.getId(), warehouse.getId())
                        .with(RestControllerTestSupport.actor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(seller.getId()))
                .andExpect(jsonPath("$.warehouseIds[0]").value(warehouse.getId()));
    }

    @Test
    void listUsersReturnsOk() throws Exception {
        User user = DomainFixtures.user(UserRole.BUYER);
        when(tellerEmployeePort.listUsers(admin)).thenReturn(List.of(user));

        mockMvc.perform(get("/api/v1/admin/users")
                        .with(RestControllerTestSupport.actor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(user.getId()))
                .andExpect(jsonPath("$[0].role").value("BUYER"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void createUserReturnsCreated() throws Exception {
        User created = DomainFixtures.user(UserRole.SUPERVISOR);
        when(tellerEmployeePort.createUser(
                admin,
                FullName.of("Jane Doe"),
                Email.of("jane@example.com"),
                IdentityDocument.of("DOC12345"),
                UserRole.SUPERVISOR,
                "secret123")).thenReturn(created);

        mockMvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_USER_JSON)
                        .with(RestControllerTestSupport.actor(admin)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.role").value("SUPERVISOR"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void blockUserReturnsOk() throws Exception {
        User blocked = DomainFixtures.user(UserRole.BUYER);
        blocked.block();
        when(tellerEmployeePort.blockUser(admin, blocked.getId())).thenReturn(blocked);

        mockMvc.perform(post("/api/v1/admin/users/{id}/block", blocked.getId())
                        .with(RestControllerTestSupport.actor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(blocked.getId()))
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    void activateUserReturnsOk() throws Exception {
        User activated = DomainFixtures.user(UserRole.BUYER);
        activated.deactivate();
        activated.activate();
        when(tellerEmployeePort.activateUser(admin, activated.getId())).thenReturn(activated);

        mockMvc.perform(post("/api/v1/admin/users/{id}/activate", activated.getId())
                        .with(RestControllerTestSupport.actor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activated.getId()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void deactivateUserReturnsOk() throws Exception {
        User deactivated = DomainFixtures.user(UserRole.BUYER);
        deactivated.deactivate();
        when(tellerEmployeePort.deactivateUser(admin, deactivated.getId()))
                .thenReturn(deactivated);

        mockMvc.perform(post("/api/v1/admin/users/{id}/deactivate", deactivated.getId())
                        .with(RestControllerTestSupport.actor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(deactivated.getId()))
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void listReturnsReturnsOk() throws Exception {
        Return returnRequest = returnWithStatus(ReturnStatus.REQUESTED);
        when(tellerEmployeePort.listReturns(admin)).thenReturn(List.of(returnRequest));

        mockMvc.perform(get("/api/v1/admin/returns")
                        .with(RestControllerTestSupport.actor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(returnRequest.getId()))
                .andExpect(jsonPath("$[0].orderId").value(returnRequest.getOrder().getId()))
                .andExpect(jsonPath("$[0].reason").value("Item arrived damaged"))
                .andExpect(jsonPath("$[0].status").value("REQUESTED"));
    }

    @Test
    void approveReturnReturnsOk() throws Exception {
        Return returnRequest = returnWithStatus(ReturnStatus.APPROVED);
        when(tellerEmployeePort.approveReturn(admin, returnRequest.getId()))
                .thenReturn(returnRequest);

        mockMvc.perform(post("/api/v1/admin/returns/{id}/approve", returnRequest.getId())
                        .with(RestControllerTestSupport.actor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(returnRequest.getId()))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void rejectReturnReturnsOk() throws Exception {
        Return returnRequest = returnWithStatus(ReturnStatus.REJECTED);
        when(tellerEmployeePort.rejectReturn(admin, returnRequest.getId()))
                .thenReturn(returnRequest);

        mockMvc.perform(post("/api/v1/admin/returns/{id}/reject", returnRequest.getId())
                        .with(RestControllerTestSupport.actor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(returnRequest.getId()))
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void generateRefundReturnsCreated() throws Exception {
        Refund refund = refundWithStatus(RefundStatus.PENDING);
        when(tellerEmployeePort.generateRefund(eq(admin), eq(refund.getReturnRequest().getId()),
                eq(Money.of(new BigDecimal("50.00"))))).thenReturn(refund);

        mockMvc.perform(post("/api/v1/admin/returns/{id}/refunds", refund.getReturnRequest().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50.00}")
                        .with(RestControllerTestSupport.actor(admin)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(refund.getId()))
                .andExpect(jsonPath("$.returnId").value(refund.getReturnRequest().getId()))
                .andExpect(jsonPath("$.amount.amount").value(50.00))
                .andExpect(jsonPath("$.amount.currency").value("COP"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void processRefundReturnsOk() throws Exception {
        Refund refund = refundWithStatus(RefundStatus.PROCESSED);
        when(tellerEmployeePort.processRefund(admin, refund.getId())).thenReturn(refund);

        mockMvc.perform(post("/api/v1/admin/refunds/{id}/process", refund.getId())
                        .with(RestControllerTestSupport.actor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(refund.getId()))
                .andExpect(jsonPath("$.status").value("PROCESSED"));
    }

    @Test
    void blankTaxIdReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/admin/sellers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"user-1\",\"taxId\":\"\"}")
                        .with(RestControllerTestSupport.actor(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void invalidRoleCodeReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Jane Doe\",\"email\":\"jane@example.com\","
                                + "\"identityDocument\":\"DOC12345\",\"role\":\"WIZARD\","
                                + "\"password\":\"secret123\"}")
                        .with(RestControllerTestSupport.actor(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_DATA"));
    }

    @Test
    void selfRegistrationReturnsForbidden() throws Exception {
        when(tellerEmployeePort.registerSeller(any(User.class), anyString(), any(TaxId.class)))
                .thenThrow(new SelfRegistrationNotAllowedException(
                        "Only ADMIN can register sellers"));

        mockMvc.perform(post("/api/v1/admin/sellers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SELLER_JSON)
                        .with(RestControllerTestSupport.actor(admin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("SELF_REGISTRATION_NOT_ALLOWED"));
    }

    @Test
    void invalidRoleFromPortReturnsForbidden() throws Exception {
        when(tellerEmployeePort.createUser(any(User.class), any(FullName.class),
                any(Email.class), any(IdentityDocument.class), any(UserRole.class), anyString()))
                .thenThrow(new InvalidRoleException("Invalid role for new user"));

        mockMvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_USER_JSON.replace("SUPERVISOR", "BUYER"))
                        .with(RestControllerTestSupport.actor(admin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("INVALID_ROLE"));
    }

    @Test
    void missingSellerReturnsNotFound() throws Exception {
        when(tellerEmployeePort.updateSellerTaxId(eq(admin), eq("missing"),
                eq(TaxId.of("900987654-3"))))
                .thenThrow(new SellerNotFoundException("Seller not found"));

        mockMvc.perform(put("/api/v1/admin/sellers/{id}", "missing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_SELLER_JSON)
                        .with(RestControllerTestSupport.actor(admin)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void duplicateSellerReturnsConflict() throws Exception {
        when(tellerEmployeePort.registerSeller(any(User.class), anyString(), any(TaxId.class)))
                .thenThrow(new DuplicateEntityException("Seller already exists"));

        mockMvc.perform(post("/api/v1/admin/sellers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SELLER_JSON)
                        .with(RestControllerTestSupport.actor(admin)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    void invalidWarehouseTypeCodeReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/admin/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_WAREHOUSE_JSON.replace("SELLER", "MOON"))
                        .with(RestControllerTestSupport.actor(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_DATA"));
    }

    private Return returnWithStatus(ReturnStatus status) {
        Order order = Order.reconstitute(
                "order-1",
                DomainFixtures.buyer(),
                List.of(),
                OrderStatus.DELIVERED,
                Money.cop(new BigDecimal("100.00")),
                PaymentStatus.CONFIRMED,
                List.of(),
                DomainFixtures.address(),
                null,
                Instant.parse("2026-01-01T10:00:00Z"));
        return Return.reconstitute("return-1", order, "Item arrived damaged",
                status, REQUESTED_AT);
    }

    private Refund refundWithStatus(RefundStatus status) {
        Return returnRequest = returnWithStatus(ReturnStatus.APPROVED);
        return Refund.reconstitute("refund-1", returnRequest,
                Money.cop(new BigDecimal("50.00")), status);
    }
}
