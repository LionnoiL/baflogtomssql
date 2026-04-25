package ua.haponov.services;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ua.haponov.dto.*;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
public class DictionaryService {

    private final JdbcTemplate jdbcTemplate;
    private final List<Integer> infoBaseEventIds = new CopyOnWriteArrayList<>();
    private final List<Integer> sessionEventIds = new CopyOnWriteArrayList<>();

    public void refreshEventCache() {
        List<Integer> ids = jdbcTemplate.queryForList(
                "SELECT event_id FROM EventNames WHERE event_code LIKE '%InfoBase%'",
                Integer.class
        );
        infoBaseEventIds.clear();
        infoBaseEventIds.addAll(ids);

        ids = jdbcTemplate.queryForList(
                "SELECT event_id FROM EventNames WHERE event_code LIKE '%Session%'",
                Integer.class
        );
        sessionEventIds.clear();
        sessionEventIds.addAll(ids);
    }

    public List<Integer> getInfoBaseEventIds() {
        if (infoBaseEventIds.isEmpty()) {
            refreshEventCache();
        }
        return infoBaseEventIds;
    }

    public List<Integer> getSessionEventIds() {
        if (sessionEventIds.isEmpty()) {
            refreshEventCache();
        }
        return sessionEventIds;
    }

    public List<User> getUsers() {
        return jdbcTemplate.query(
                "SELECT user_id as id, user_name as name FROM Users ORDER BY name",
                new BeanPropertyRowMapper<>(User.class)
        );
    }

    public List<Event> getEventNames() {
        return jdbcTemplate.query(
                "SELECT event_id as id, event_code as code, event_human_name as name FROM EventNames ORDER BY name",
                new BeanPropertyRowMapper<>(Event.class)
        );
    }

    public List<Computer> getComputers() {
        return jdbcTemplate.query("SELECT computer_id as id, computer_name as name FROM Computers ORDER BY computer_name",
                new BeanPropertyRowMapper<>(Computer.class)
        );
    }

    public List<Application> getApplications() {
        return jdbcTemplate.query("SELECT app_id as id, app_name as name FROM Applications ORDER BY app_name",
                new BeanPropertyRowMapper<>(Application.class)
        );
    }

    public List<Metadata> getMetadata() {
        return jdbcTemplate.query("SELECT metadata_id as id, metadata_name as name FROM Metadata ORDER BY metadata_name",
                new BeanPropertyRowMapper<>(Metadata.class));
    }

    public List<SeverityLevel> getSeverityLevels() {
        return jdbcTemplate.query(
                "SELECT severity_id as id, severity_name as name, severity_color as color FROM SeverityLevels ORDER BY severity_id",
                new BeanPropertyRowMapper<>(SeverityLevel.class)
        );
    }
}
