package project.discord.backend.runtime.dto;

public record RuntimeHostMetricsResponse(
        Long diskTotalBytes,
        Long diskUsedBytes,
        Long diskAvailableBytes,
        Integer diskUsedPercent,
        Long memoryTotalBytes,
        Long memoryUsedBytes,
        Long memoryAvailableBytes,
        Long swapTotalBytes,
        Long swapUsedBytes,
        Double loadOneMinute,
        String uptime
) {
}
