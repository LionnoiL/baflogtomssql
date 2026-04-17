package ua.haponov.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsService {

    private final JdbcTemplate jdbcTemplate;

    public String getSetting(String key, String defaultValue) {
        try {
            String sql = "SELECT setting_value FROM Settings WHERE setting_key = ?";
            return jdbcTemplate.queryForObject(sql, String.class, key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public Map<String, String> getAllSettings() {
        String sql = "SELECT setting_key, setting_value FROM Settings";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
                Map.entry(rs.getString("setting_key"), rs.getString("setting_value"))
        ).stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void setSetting(String key, String value) {
        String sql = """
                MERGE INTO Settings AS target
                USING (SELECT ? AS setting_key, ? AS setting_value) AS source
                ON (target.setting_key = source.setting_key)
                WHEN MATCHED THEN
                    UPDATE SET setting_value = source.setting_value
                WHEN NOT MATCHED THEN
                    INSERT (setting_key, setting_value)
                    VALUES (source.setting_key, source.setting_value);
                """;
        jdbcTemplate.update(sql, key, value);
        log.info("Setting {} = {}", key, value);
    }

    public List<Integer> getExcludedEventIds() {
        String value = getSetting("cleanup_excluded_event_ids", "");
        if (value.isBlank()) return List.of();
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    public boolean isCleanupEnabled() {
        return Boolean.parseBoolean(getSetting("cleanup_enabled", "false"));
    }

    public boolean isCleanupSQLiteEnabled() {
        return Boolean.parseBoolean(getSetting("cleanup_sqlite_enabled", "false"));
    }

    public int getCleanupDays() {
        return Integer.parseInt(getSetting("cleanup_days", "365"));
    }
}
