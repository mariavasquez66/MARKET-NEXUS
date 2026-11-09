package application.infrastructure.exception;

import application.adapters.rest.dto.response.ErrorResponse;
import application.domain.exceptions.buyer.BuyerNotFoundException;
import application.domain.exceptions.buyer.InvalidAddressException;
import application.domain.exceptions.buyer.RestrictedBuyerException;
import application.domain.exceptions.cart.CartNotFoundException;
import application.domain.exceptions.cart.EmptyCartException;
import application.domain.exceptions.cart.InvalidQuantityException;
import application.domain.exceptions.cart.ProductNotAvailableException;
import application.domain.exceptions.common.DomainException;
import application.domain.exceptions.common.DuplicateEntityException;
import application.domain.exceptions.common.InsufficientPermissionException;
import application.domain.exceptions.common.InvalidDataException;
import application.domain.exceptions.common.InvalidDateRangeException;
import application.domain.exceptions.common.NoDataException;
import application.domain.exceptions.common.SingleRoleException;
import application.domain.exceptions.common.UnauthenticatedUserException;
import application.domain.exceptions.common.UnauthorizedOperationException;
import application.domain.exceptions.inventory.DamagedInventoryException;
import application.domain.exceptions.inventory.InsufficientStockException;
import application.domain.exceptions.inventory.InvalidReservationException;
import application.domain.exceptions.inventory.InventoryNotFoundException;
import application.domain.exceptions.inventory.NegativeStockException;
import application.domain.exceptions.invoice.InvoiceAlreadyExistsException;
import application.domain.exceptions.invoice.InvoiceNotFoundException;
import application.domain.exceptions.invoice.VoidedInvoiceException;
import application.domain.exceptions.logistics.InvalidDestinationAddressException;
import application.domain.exceptions.logistics.ShipmentAlreadyStartedException;
import application.domain.exceptions.logistics.ShipmentNotFoundException;
import application.domain.exceptions.order.CancelledOrderException;
import application.domain.exceptions.order.FinalizedOrderException;
import application.domain.exceptions.order.InvalidStateTransitionException;
import application.domain.exceptions.order.OrderNotFoundException;
import application.domain.exceptions.product.InvalidPriceException;
import application.domain.exceptions.product.InvalidProductStateException;
import application.domain.exceptions.product.InvalidProductTypeException;
import application.domain.exceptions.product.ProductNotFoundException;
import application.domain.exceptions.product.ProductNotPublishedException;
import application.domain.exceptions.refund.InvalidRefundAmountException;
import application.domain.exceptions.refund.RefundAlreadyProcessedException;
import application.domain.exceptions.refund.RefundNotFoundException;
import application.domain.exceptions.returns.ReturnAlreadyProcessedException;
import application.domain.exceptions.returns.ReturnNotAllowedException;
import application.domain.exceptions.returns.ReturnNotFoundException;
import application.domain.exceptions.seller.SelfRegistrationNotAllowedException;
import application.domain.exceptions.seller.SellerNotFoundException;
import application.domain.exceptions.seller.SellerWithoutWarehouseException;
import application.domain.exceptions.user.DuplicateEmailException;
import application.domain.exceptions.user.DuplicateIdentificationException;
import application.domain.exceptions.user.InactiveUserException;
import application.domain.exceptions.user.InvalidRoleException;
import application.domain.exceptions.user.UserNotFoundException;
import application.domain.exceptions.warehouse.InvalidWarehouseTypeException;
import application.domain.exceptions.warehouse.WarehouseAlreadyAssignedException;
import application.domain.exceptions.warehouse.WarehouseNotFoundException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        if (message.isBlank()) {
            message = ex.getBindingResult().getGlobalErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining(", "));
        }
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                message.isBlank() ? "Validation failed" : message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .collect(Collectors.joining(", "));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                message.isBlank() ? "Validation failed" : message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Malformed request body", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "Invalid value for parameter '" + ex.getName() + "'", request);
    }

    @ExceptionHandler({JwtException.class, SecurityException.class})
    public ResponseEntity<ErrorResponse> handleInvalidToken(
            RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Invalid or expired token", request);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(
            DomainException ex, HttpServletRequest request) {
        ErrorSpec spec = classify(ex);
        return build(spec.status(), spec.code(), ex.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation", ex);
        return build(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE",
                "Resource conflicts with existing data", request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(
            OptimisticLockingFailureException ex, HttpServletRequest request) {
        log.warn("Concurrent modification detected", ex);
        return build(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "Resource was modified concurrently; retry the operation", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", request);
    }

    private ErrorSpec classify(DomainException ex) {
        if (ex instanceof BuyerNotFoundException
                || ex instanceof CartNotFoundException
                || ex instanceof InventoryNotFoundException
                || ex instanceof InvoiceNotFoundException
                || ex instanceof OrderNotFoundException
                || ex instanceof ProductNotFoundException
                || ex instanceof RefundNotFoundException
                || ex instanceof ReturnNotFoundException
                || ex instanceof SellerNotFoundException
                || ex instanceof ShipmentNotFoundException
                || ex instanceof UserNotFoundException
                || ex instanceof WarehouseNotFoundException) {
            return new ErrorSpec(HttpStatus.NOT_FOUND, "NOT_FOUND");
        }
        if (ex instanceof NoDataException) {
            return new ErrorSpec(HttpStatus.NOT_FOUND, "NO_DATA");
        }
        if (ex instanceof ProductNotPublishedException) {
            return new ErrorSpec(HttpStatus.NOT_FOUND, "PRODUCT_NOT_PUBLISHED");
        }
        if (ex instanceof InvalidDataException
                || ex instanceof InvalidWarehouseTypeException) {
            return badRequest("INVALID_DATA");
        }
        if (ex instanceof InvalidDateRangeException) {
            return badRequest("INVALID_DATE_RANGE");
        }
        if (ex instanceof InvalidAddressException) {
            return badRequest("INVALID_ADDRESS");
        }
        if (ex instanceof InvalidPriceException) {
            return badRequest("INVALID_PRICE");
        }
        if (ex instanceof InvalidQuantityException) {
            return badRequest("INVALID_QUANTITY");
        }
        if (ex instanceof InvalidProductTypeException) {
            return badRequest("INVALID_PRODUCT_TYPE");
        }
        if (ex instanceof InvalidDestinationAddressException) {
            return badRequest("INVALID_DESTINATION_ADDRESS");
        }
        if (ex instanceof EmptyCartException) {
            return badRequest("EMPTY_CART");
        }
        if (ex instanceof UnauthenticatedUserException) {
            return new ErrorSpec(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED");
        }
        if (ex instanceof InsufficientPermissionException
                || ex instanceof RestrictedBuyerException) {
            return new ErrorSpec(HttpStatus.FORBIDDEN, "FORBIDDEN");
        }
        if (ex instanceof UnauthorizedOperationException) {
            return new ErrorSpec(HttpStatus.FORBIDDEN, "UNAUTHORIZED_OPERATION");
        }
        if (ex instanceof InactiveUserException) {
            return new ErrorSpec(HttpStatus.FORBIDDEN, "INACTIVE_USER");
        }
        if (ex instanceof SelfRegistrationNotAllowedException) {
            return new ErrorSpec(HttpStatus.FORBIDDEN, "SELF_REGISTRATION_NOT_ALLOWED");
        }
        if (ex instanceof SingleRoleException) {
            return new ErrorSpec(HttpStatus.FORBIDDEN, "SINGLE_ROLE_VIOLATION");
        }
        if (ex instanceof InvalidRoleException) {
            return new ErrorSpec(HttpStatus.FORBIDDEN, "INVALID_ROLE");
        }
        if (ex instanceof DuplicateEntityException
                || ex instanceof DuplicateEmailException
                || ex instanceof DuplicateIdentificationException) {
            return new ErrorSpec(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE");
        }
        if (ex instanceof InvalidStateTransitionException) {
            return new ErrorSpec(HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION");
        }
        if (ex instanceof InvalidProductStateException) {
            return new ErrorSpec(HttpStatus.CONFLICT, "INVALID_PRODUCT_STATE");
        }
        if (ex instanceof FinalizedOrderException) {
            return new ErrorSpec(HttpStatus.CONFLICT, "FINALIZED_ORDER");
        }
        if (ex instanceof CancelledOrderException) {
            return new ErrorSpec(HttpStatus.CONFLICT, "CANCELLED_ORDER");
        }
        if (ex instanceof InsufficientStockException) {
            return new ErrorSpec(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK");
        }
        if (ex instanceof NegativeStockException) {
            return new ErrorSpec(HttpStatus.CONFLICT, "NEGATIVE_STOCK");
        }
        if (ex instanceof DamagedInventoryException) {
            return new ErrorSpec(HttpStatus.CONFLICT, "DAMAGED_INVENTORY");
        }
        if (ex instanceof InvalidReservationException) {
            return new ErrorSpec(HttpStatus.CONFLICT, "INVALID_RESERVATION");
        }
        if (ex instanceof WarehouseAlreadyAssignedException) {
            return new ErrorSpec(HttpStatus.CONFLICT, "WAREHOUSE_ALREADY_ASSIGNED");
        }
        if (ex instanceof SellerWithoutWarehouseException) {
            return new ErrorSpec(HttpStatus.CONFLICT, "SELLER_WITHOUT_WAREHOUSE");
        }
        if (ex instanceof InvoiceAlreadyExistsException) {
            return new ErrorSpec(HttpStatus.CONFLICT, "INVOICE_ALREADY_EXISTS");
        }
        if (ex instanceof VoidedInvoiceException) {
            return new ErrorSpec(HttpStatus.CONFLICT, "VOIDED_INVOICE");
        }
        if (ex instanceof ShipmentAlreadyStartedException) {
            return new ErrorSpec(HttpStatus.CONFLICT, "SHIPMENT_ALREADY_STARTED");
        }
        if (ex instanceof ReturnNotAllowedException) {
            return new ErrorSpec(HttpStatus.CONFLICT, "RETURN_NOT_ALLOWED");
        }
        if (ex instanceof ReturnAlreadyProcessedException) {
            return new ErrorSpec(HttpStatus.CONFLICT, "RETURN_ALREADY_PROCESSED");
        }
        if (ex instanceof RefundAlreadyProcessedException) {
            return new ErrorSpec(HttpStatus.CONFLICT, "REFUND_ALREADY_PROCESSED");
        }
        if (ex instanceof InvalidRefundAmountException) {
            return new ErrorSpec(HttpStatus.CONFLICT, "INVALID_REFUND_AMOUNT");
        }
        if (ex instanceof ProductNotAvailableException) {
            return new ErrorSpec(HttpStatus.CONFLICT, "PRODUCT_NOT_AVAILABLE");
        }
        return badRequest("INVALID_DATA");
    }

    private ErrorSpec badRequest(String code) {
        return new ErrorSpec(HttpStatus.BAD_REQUEST, code);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status,
                                                String code,
                                                String message,
                                                HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                status.value(),
                code,
                message,
                request.getRequestURI(),
                resolveRequestId(request));
        return ResponseEntity.status(status).body(body);
    }

    private String resolveRequestId(HttpServletRequest request) {
        Object requestId = request.getAttribute("requestId");
        if (requestId instanceof String value && !value.isBlank()) {
            return value;
        }
        return UUID.randomUUID().toString();
    }

    private record ErrorSpec(HttpStatus status, String code) {
    }
}
