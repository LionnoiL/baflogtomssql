package ua.haponov.dto.reports;

import java.util.List;
import java.util.Map;

public record SummaryStatsDto(
        long totalEventsToday,
        long errorCountToday,
        long activeUsersCount,
        List<Map<String, Object>> topErrors
) {
}