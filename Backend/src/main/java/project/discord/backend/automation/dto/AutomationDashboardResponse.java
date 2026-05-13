package project.discord.backend.automation.dto;

import java.util.List;

public record AutomationDashboardResponse(
        Boolean enabled,
        Boolean runtimeSuspendEnabled,
        Integer reminderDaysBefore,
        Integer pastDueGraceDays,
        Integer cancelGraceDays,
        Integer activeBillingSubscriptions,
        Integer pastDueBillingSubscriptions,
        Integer activeFeatureSubscriptions,
        Integer pastDueFeatureSubscriptions,
        List<AutomationSettingResponse> settings,
        List<AutomationRunResponse> recentRuns
) {
}
