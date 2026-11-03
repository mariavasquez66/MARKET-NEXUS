package application.adapters.rest.controllers;

import application.adapters.rest.dto.request.CreateUserRequest;
import application.adapters.rest.dto.request.CreateWarehouseRequest;
import application.adapters.rest.dto.request.GenerateRefundRequest;
import application.adapters.rest.dto.request.RegisterSellerRequest;
import application.adapters.rest.dto.request.UpdateSellerRequest;
import application.adapters.rest.dto.request.UpdateWarehouseRequest;
import application.adapters.rest.dto.response.RefundResponse;
import application.adapters.rest.dto.response.ReturnResponse;
import application.adapters.rest.dto.response.SellerResponse;
import application.adapters.rest.dto.response.UserResponse;
import application.adapters.rest.dto.response.WarehouseResponse;
import application.adapters.rest.mappers.RefundRestMapper;
import application.adapters.rest.mappers.ReturnRestMapper;
import application.adapters.rest.mappers.SellerRestMapper;
import application.adapters.rest.mappers.UserRestMapper;
import application.adapters.rest.mappers.WarehouseRestMapper;
import application.domain.enums.UserRole;
import application.domain.enums.WarehouseType;
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
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@Validated
public class AdminController {

    private final TellerEmployeePort tellerEmployeePort;
    private final SellerRestMapper sellerRestMapper;
    private final WarehouseRestMapper warehouseRestMapper;
    private final UserRestMapper userRestMapper;
    private final ReturnRestMapper returnRestMapper;
    private final RefundRestMapper refundRestMapper;

    public AdminController(TellerEmployeePort tellerEmployeePort,
                           SellerRestMapper sellerRestMapper,
                           WarehouseRestMapper warehouseRestMapper,
                           UserRestMapper userRestMapper,
                           ReturnRestMapper returnRestMapper,
                           RefundRestMapper refundRestMapper) {
        this.tellerEmployeePort = tellerEmployeePort;
        this.sellerRestMapper = sellerRestMapper;
        this.warehouseRestMapper = warehouseRestMapper;
        this.userRestMapper = userRestMapper;
        this.returnRestMapper = returnRestMapper;
        this.refundRestMapper = refundRestMapper;
    }

    @PostMapping("/sellers")
    public ResponseEntity<SellerResponse> registerSeller(
            @Valid @RequestBody RegisterSellerRequest request,
            @AuthenticationPrincipal User actor) {
        Seller seller = tellerEmployeePort.registerSeller(
                actor, request.userId(), TaxId.of(request.taxId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(sellerRestMapper.toResponse(seller));
    }

    @GetMapping("/sellers")
    public ResponseEntity<List<SellerResponse>> listSellers(
            @AuthenticationPrincipal User actor) {
        List<SellerResponse> response = tellerEmployeePort.listSellers(actor).stream()
                .map(sellerRestMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/sellers/{id}")
    public ResponseEntity<SellerResponse> updateSeller(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateSellerRequest request,
            @AuthenticationPrincipal User actor) {
        Seller seller = tellerEmployeePort.updateSellerTaxId(actor, id, TaxId.of(request.taxId()));
        return ResponseEntity.ok(sellerRestMapper.toResponse(seller));
    }

    @PostMapping("/warehouses")
    public ResponseEntity<WarehouseResponse> createWarehouse(
            @Valid @RequestBody CreateWarehouseRequest request,
            @AuthenticationPrincipal User actor) {
        Address location = Address.of(
                request.location().street(),
                request.location().city(),
                request.location().region(),
                request.location().postalCode(),
                request.location().country());
        Warehouse warehouse = tellerEmployeePort.createWarehouse(
                actor,
                request.name(),
                location,
                WarehouseType.fromCode(request.type()),
                request.ownerSellerId(),
                request.responsibleUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(warehouseRestMapper.toResponse(warehouse));
    }

    @GetMapping("/warehouses")
    public ResponseEntity<List<WarehouseResponse>> listWarehouses(
            @AuthenticationPrincipal User actor) {
        List<WarehouseResponse> response = tellerEmployeePort.listWarehouses(actor).stream()
                .map(warehouseRestMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/warehouses/{id}")
    public ResponseEntity<WarehouseResponse> updateWarehouse(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateWarehouseRequest request,
            @AuthenticationPrincipal User actor) {
        Address location = Address.of(
                request.location().street(),
                request.location().city(),
                request.location().region(),
                request.location().postalCode(),
                request.location().country());
        Warehouse warehouse = tellerEmployeePort.updateWarehouse(
                actor, id, request.name(), location, request.responsibleUserId());
        return ResponseEntity.ok(warehouseRestMapper.toResponse(warehouse));
    }

    @PostMapping("/sellers/{id}/warehouses/{warehouseId}")
    public ResponseEntity<SellerResponse> assignWarehouse(
            @PathVariable("id") String id,
            @PathVariable("warehouseId") String warehouseId,
            @AuthenticationPrincipal User actor) {
        Seller seller = tellerEmployeePort.assignWarehouse(actor, id, warehouseId);
        return ResponseEntity.ok(sellerRestMapper.toResponse(seller));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> listUsers(
            @AuthenticationPrincipal User actor) {
        List<UserResponse> response = tellerEmployeePort.listUsers(actor).stream()
                .map(userRestMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal User actor) {
        User user = tellerEmployeePort.createUser(
                actor,
                FullName.of(request.fullName()),
                Email.of(request.email()),
                IdentityDocument.of(request.identityDocument()),
                UserRole.fromCode(request.role()),
                request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(userRestMapper.toResponse(user));
    }

    @PostMapping("/users/{id}/block")
    public ResponseEntity<UserResponse> blockUser(
            @PathVariable("id") String id,
            @AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(userRestMapper.toResponse(tellerEmployeePort.blockUser(actor, id)));
    }

    @PostMapping("/users/{id}/activate")
    public ResponseEntity<UserResponse> activateUser(
            @PathVariable("id") String id,
            @AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(userRestMapper.toResponse(tellerEmployeePort.activateUser(actor, id)));
    }

    @PostMapping("/users/{id}/deactivate")
    public ResponseEntity<UserResponse> deactivateUser(
            @PathVariable("id") String id,
            @AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(userRestMapper.toResponse(tellerEmployeePort.deactivateUser(actor, id)));
    }

    @GetMapping("/returns")
    public ResponseEntity<List<ReturnResponse>> listReturns(
            @AuthenticationPrincipal User actor) {
        List<ReturnResponse> response = tellerEmployeePort.listReturns(actor).stream()
                .map(returnRestMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/returns/{id}/approve")
    public ResponseEntity<ReturnResponse> approveReturn(
            @PathVariable("id") String id,
            @AuthenticationPrincipal User actor) {
        Return returnRequest = tellerEmployeePort.approveReturn(actor, id);
        return ResponseEntity.ok(returnRestMapper.toResponse(returnRequest));
    }

    @PostMapping("/returns/{id}/reject")
    public ResponseEntity<ReturnResponse> rejectReturn(
            @PathVariable("id") String id,
            @AuthenticationPrincipal User actor) {
        Return returnRequest = tellerEmployeePort.rejectReturn(actor, id);
        return ResponseEntity.ok(returnRestMapper.toResponse(returnRequest));
    }

    @PostMapping("/returns/{id}/refunds")
    public ResponseEntity<RefundResponse> generateRefund(
            @PathVariable("id") String id,
            @Valid @RequestBody GenerateRefundRequest request,
            @AuthenticationPrincipal User actor) {
        Refund refund = tellerEmployeePort.generateRefund(actor, id, Money.of(request.amount()));
        return ResponseEntity.status(HttpStatus.CREATED).body(refundRestMapper.toResponse(refund));
    }

    @PostMapping("/refunds/{id}/process")
    public ResponseEntity<RefundResponse> processRefund(
            @PathVariable("id") String id,
            @AuthenticationPrincipal User actor) {
        Refund refund = tellerEmployeePort.processRefund(actor, id);
        return ResponseEntity.ok(refundRestMapper.toResponse(refund));
    }
}
