package ua.haponov.dto.reports;

public record IntraDiurnalActivityDto(
    String date,
    Integer eventCount,
    Integer uniqueUsers,
    Integer sessionsCount
) {
}
