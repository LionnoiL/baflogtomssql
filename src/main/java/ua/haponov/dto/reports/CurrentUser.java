package ua.haponov.dto.reports;

public record CurrentUser(
        String name,
        String uuid,
        String sessionStartTime,
        int sessionId
) {
}
