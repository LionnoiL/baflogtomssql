package ua.haponov.dto.reports;


public record BackgroundTaskDto(
        String name,
        String start,
        String end,
        Long duration
) {
}
