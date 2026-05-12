package project.discord.backend.customer.dto;

import java.util.List;

public record CustomerDashboardResponse(
        DashboardSummaryResponse summary,
        BillingSummaryResponse billing,
        List<BotResponse> bots,
        List<FeatureResponse> availableFeatures
) {
}
