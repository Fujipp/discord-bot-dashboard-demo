package project.discord.backend.payment.dto;

public record CheckoutItemResponse(
        Long featureId,
        String code,
        String name,
        Integer amountCents,
        String currency
) {
}
