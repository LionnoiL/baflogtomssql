package ua.haponov.dto.reports;

public record AuthAuditDto(
        String eventDate,
        String userName,
        String computerName,
        String appName,
        String authStatus,
        String severityName,
        String comment,
        String data
) {
}