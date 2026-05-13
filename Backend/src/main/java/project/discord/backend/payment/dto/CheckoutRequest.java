package project.discord.backend.payment.dto;

import java.util.List;

public record CheckoutRequest(
        Long botId,
        String packCode,
        List<Long> featureIds
) {
}
