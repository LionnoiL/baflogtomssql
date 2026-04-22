package ua.haponov.dto.reports;

public record AdministrativeActionDto(
        String eventDate,
        String userName,
        String computerName,
        String eventHumanName,
        String appName,
        String comment,
        String dataInfo
) {
}
