package ua.haponov.dto.reports;

public record ActivityMatrixDto(
        int hour,
        long mon,
        long tue,
        long wed,
        long thu,
        long fri,
        long sat,
        long sun,
        long totalPerHour
) {
}
