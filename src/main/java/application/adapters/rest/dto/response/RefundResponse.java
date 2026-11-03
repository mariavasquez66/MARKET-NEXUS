package application.adapters.rest.dto.response;

public record RefundResponse(
        String id,
        String returnId,
        MoneyResponse amount,
        String status) {
}
