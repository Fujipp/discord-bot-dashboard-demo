package project.discord.backend.automation.dto;

import java.time.Instant;

public record AutomationRunResponse(
        Long id,
        String runType,
        String status,
        Integer billingMarkedPastDue,
        Integer featureMarkedPastDue,
        Integer featureCanceled,
        Integer runtimeSuspended,
        Integer notificationsCreated,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt
) {
}
