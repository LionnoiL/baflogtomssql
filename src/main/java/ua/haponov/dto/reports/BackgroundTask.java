package ua.haponov.dto.reports;


public record BackgroundTask(
        String name,
        String start,
        String end,
        Long duration
) {
}
