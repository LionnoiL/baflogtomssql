package ua.haponov.dto.reports;

public record Suspicion(
        String userName,
        String eventHumanName,
        String appName,
        String computerName,
        String eventDate,
        String suspicionReason,
        String comment,
        String dataInfo
) {
}
