package ua.haponov.dto.reports;

public record CurrentUserDto(
        String name,
        String uuid,
        String sessionStartTime,
        int sessionId
) {
}
