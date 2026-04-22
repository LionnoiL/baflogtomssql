package ua.haponov.dto.reports;

public record IntraDiurnalActivityDto(
    String date,
    String dayOfWeek,
    Integer eventCount,
    Integer uniqueUsers,
    Integer sessionsCount
) {
}
