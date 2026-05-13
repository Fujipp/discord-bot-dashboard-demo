package project.discord.backend.payment.dto;

import java.time.Instant;
import java.util.List;

public record CheckoutResponse(
        Long paymentId,
        String checkoutReference,
        String provider,
        String providerPaymentId,
        String status,
        Integer amountCents,
        String currency,
        String qrCodeUrl,
        Instant expiresAt,
        List<CheckoutItemResponse> items
) {
}
