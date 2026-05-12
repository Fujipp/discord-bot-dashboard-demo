package project.discord.backend.customer.dto;

public record DashboardSummaryResponse(
        Integer botCount,
        Integer onlineBotCount,
        Integer connectedServerCount,
        Integer commandCount,
        Integer activeFeatureCount
) {
}
