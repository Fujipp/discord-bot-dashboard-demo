package project.discord.backend.admin.dto;

import java.util.List;

import project.discord.backend.runtime.dto.RuntimeHostMetricsResponse;
import project.discord.backend.runtime.dto.RuntimeHostOptionResponse;

public record AdminRuntimeDashboardResponse(
        String selectedHostId,
        List<RuntimeHostOptionResponse> hosts,
        RuntimeHostMetricsResponse host,
        List<AdminRuntimeProcessResponse> processes,
        List<AdminUserOptionResponse> users
) {
}
