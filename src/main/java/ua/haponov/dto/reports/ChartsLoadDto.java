package ua.haponov.dto.reports;

import java.util.List;

public record ChartsLoadDto(
        List<LoadStatsDto> loadStats,
        List<IntraDiurnalActivityDto> intraDiurnalActivity
) {
}
