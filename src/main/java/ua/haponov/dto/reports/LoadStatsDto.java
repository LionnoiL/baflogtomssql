package ua.haponov.dto.reports;

import java.math.BigDecimal;

public record LoadStatsDto(
        Integer hour,
        Long eventCount,
        BigDecimal loadPercentage
) {
}
