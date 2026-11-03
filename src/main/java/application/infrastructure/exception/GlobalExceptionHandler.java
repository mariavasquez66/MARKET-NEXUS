package application.infrastructure.exception;

import application.adapters.rest.dto.response.ErrorResponse;
import application.domain.exceptions.common.DomainException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", request);
    }

    private ErrorSpec classify(DomainException ex) {
        String name = ex.getClass().getSimpleName();
        if (name.endsWith("NotFoundException")) {
            return new ErrorSpec(HttpStatus.NOT_FOUND, "NOT_FOUND");
        }
        return switch (name) {
            case "InvalidDataException" -> badRequest("INVALID_DATA");
            case "InvalidDateRangeException" -> badRequest("INVALID_DATE_RANGE");
            case "InvalidAddressException" -> badRequest("INVALID_ADDRESS");
            case "InvalidPriceException" -> badRequest("INVALID_PRICE");
            case "InvalidQuantityException" -> badRequest("INVALID_QUANTITY");
            case "InvalidProductTypeException" -> badRequest("INVALID_PRODUCT_TYPE");
            case "InvalidDestinationAddressException" -> badRequest("INVALID_DESTINATION_ADDRESS");
            case "EmptyCartException" -> badRequest("EMPTY_CART");
            case "InvalidWarehouseTypeException" -> badRequest("INVALID_DATA");
            case "UnauthenticatedUserException" ->
                    new ErrorSpec(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED");
            case "InsufficientPermissionException" ->
                    new ErrorSpec(HttpStatus.FORBIDDEN, "FORBIDDEN");
            case "RestrictedBuyerException" ->
                    new ErrorSpec(HttpStatus.FORBIDDEN, "FORBIDDEN");
            case "UnauthorizedOperationException" ->
                    new ErrorSpec(HttpStatus.FORBIDDEN, "UNAUTHORIZED_OPERATION");
            case "InactiveUserException" ->
                    new ErrorSpec(HttpStatus.FORBIDDEN, "INACTIVE_USER");
            case "SelfRegistrationNotAllowedException" ->
                    new ErrorSpec(HttpStatus.FORBIDDEN, "SELF_REGISTRATION_NOT_ALLOWED");
            case "SingleRoleException" ->
                    new ErrorSpec(HttpStatus.FORBIDDEN, "SINGLE_ROLE_VIOLATION");
            case "InvalidRoleException" ->
                    new ErrorSpec(HttpStatus.FORBIDDEN, "INVALID_ROLE");
            case "NoDataException" -> new ErrorSpec(HttpStatus.NOT_FOUND, "NO_DATA");
            case "ProductNotPublishedException" ->
                    new ErrorSpec(HttpStatus.NOT_FOUND, "PRODUCT_NOT_PUBLISHED");
            case "DuplicateEntityException", "DuplicateEmailException", "DuplicateIdentificationException" ->
                    new ErrorSpec(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE");
            case "InvalidStateTransitionException" ->
                    new ErrorSpec(HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION");
            case "InvalidProductStateException" ->
                    new ErrorSpec(HttpStatus.CONFLICT, "INVALID_PRODUCT_STATE");
            case "FinalizedOrderException" ->
                    new ErrorSpec(HttpStatus.CONFLICT, "FINALIZED_ORDER");
            case "CancelledOrderException" ->
                    new ErrorSpec(HttpStatus.CONFLICT, "CANCELLED_ORDER");
            case "InsufficientStockException" ->
                    new ErrorSpec(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK");
            case "NegativeStockException" ->
                    new ErrorSpec(HttpStatus.CONFLICT, "NEGATIVE_STOCK");
            case "DamagedInventoryException" ->
                    new ErrorSpec(HttpStatus.CONFLICT, "DAMAGED_INVENTORY");
            case "InvalidReservationException" ->
                    new ErrorSpec(HttpStatus.CONFLICT, "INVALID_RESERVATION");
            case "WarehouseAlreadyAssignedException" ->
                    new ErrorSpec(HttpStatus.CONFLICT, "WAREHOUSE_ALREADY_ASSIGNED");
            case "SellerWithoutWarehouseException" ->
                    new ErrorSpec(HttpStatus.CONFLICT, "SELLER_WITHOUT_WAREHOUSE");
            case "InvoiceAlreadyExistsException" ->
                    new ErrorSpec(HttpStatus.CONFLICT, "INVOICE_ALREADY_EXISTS");
            case "VoidedInvoiceException" ->
                    new ErrorSpec(HttpStatus.CONFLICT, "VOIDED_INVOICE");
            case "ShipmentAlreadyStartedException" ->
                    new ErrorSpec(HttpStatus.CONFLICT, "SHIPMENT_ALREADY_STARTED");
            case "ReturnNotAllowedException" ->
                    new ErrorSpec(HttpStatus.CONFLICT, "RETURN_NOT_ALLOWED");
            case "ReturnAlreadyProcessedException" ->
                    new ErrorSpec(HttpStatus.CONFLICT, "RETURN_ALREADY_PROCESSED");
            case "RefundAlreadyProcessedException" ->
                    new ErrorSpec(HttpStatus.CONFLICT, "REFUND_ALREADY_PROCESSED");
            case "InvalidRefundAmountException" ->
                    new ErrorSpec(HttpStatus.CONFLICT, "INVALID_REFUND_AMOUNT");
            case "ProductNotAvailableException" ->
                    new ErrorSpec(HttpStatus.CONFLICT, "PRODUCT_NOT_AVAILABLE");
            default -> badRequest("INVALID_DATA");
        };
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
