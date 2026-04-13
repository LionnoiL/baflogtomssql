package ua.haponov.services;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ua.haponov.dto.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DictionaryService {

    private final JdbcTemplate jdbcTemplate;

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
