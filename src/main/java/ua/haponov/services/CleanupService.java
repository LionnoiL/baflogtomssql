package ua.haponov.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CleanupService {

    private final SettingsService settingsService;
    private final LogService logService;

    @Scheduled(fixedDelayString = "${app.cleanup.interval-ms}")
    public void cleanup() {

        log.info("Starting cleanup...");

        int days = settingsService.getCleanupDays();
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(days);
        List<Integer> excludedIds = settingsService.getExcludedEventIds();

        int deletedRows = logService.deleteLogs(thresholdDate, excludedIds);

        log.info("Cleanup completed. Deleted {} rows older than {} days.", deletedRows, days);
    }
}
