package ua.haponov.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dictionaries")
@RequiredArgsConstructor
public class DictionaryController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/users")
    public List<Map<String, Object>> getUsers() {
        return jdbcTemplate.queryForList("SELECT user_id, user_name, uuid FROM Users ORDER BY user_name");
    }

    @GetMapping("/event-names")
    public List<Map<String, Object>> getEventNames() {
        return jdbcTemplate.queryForList("SELECT event_id, event_code, event_human_name FROM EventNames ORDER BY event_id");
    }

    @GetMapping("/computers")
    public List<Map<String, Object>> getComputers() {
        return jdbcTemplate.queryForList("SELECT computer_id, computer_name FROM Computers ORDER BY computer_name");
    }

    @GetMapping("/applications")
    public List<Map<String, Object>> getApplications() {
        return jdbcTemplate.queryForList("SELECT app_id, app_name FROM Applications ORDER BY app_name");
    }

    @GetMapping("/metadata")
    public List<Map<String, Object>> getMetadata() {
        return jdbcTemplate.queryForList("SELECT metadata_id, metadata_name, uuid FROM Metadata ORDER BY metadata_name");
    }

    @GetMapping("/severity-levels")
    public List<Map<String, Object>> getSeverityLevels() {
        return jdbcTemplate.queryForList("SELECT severity_id, severity_name, severity_color FROM SeverityLevels ORDER BY severity_id");
    }

    @GetMapping("/all")
    public Map<String, List<Map<String, Object>>> getAllDictionaries() {
        return Map.of(
                "users", getUsers(),
                "eventNames", getEventNames(),
                "computers", getComputers(),
                "applications", getApplications(),
                "metadata", getMetadata(),
                "severityLevels", getSeverityLevels()
        );
    }
}
