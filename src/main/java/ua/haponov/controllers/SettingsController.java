package ua.haponov.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ua.haponov.services.SettingsService;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    public Map<String, String> getAllSettings() {
        return settingsService.getAllSettings();
    }

    @PostMapping("/{key}")
    public void updateSetting(@PathVariable String key, @RequestBody(required = false) String value) {
        settingsService.setSetting(key, value == null ? "" : value);
    }

    @GetMapping("/cleanup/status")
    public Map<String, Object> getCleanupSettings() {
        return Map.of(
                "sqliteEnabled", settingsService.isCleanupSQLiteEnabled(),
                "enabled", settingsService.isCleanupEnabled(),
                "days", settingsService.getCleanupDays(),
                "excludedEventIds", settingsService.getExcludedEventIds()
        );
    }
}