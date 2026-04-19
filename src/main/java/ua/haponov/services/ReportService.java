package ua.haponov.services;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ua.haponov.dto.reports.BackgroundTask;
import ua.haponov.dto.reports.SummaryStatsDto;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final JdbcTemplate jdbcTemplate;
    private final SettingsService settingsService;

    private String createDateFilter(List<Object> params, String from, String to){
        StringBuilder dateFilter = new StringBuilder();
        if (from != null && !from.isEmpty()) {
            dateFilter.append(" AND event_date >= ?");
            params.add(from);
        } else {
            dateFilter.append(" AND event_date >= CAST(GETDATE() AS DATE)");
        }

        if (to != null && !to.isEmpty()) {
            dateFilter.append(" AND event_date <= ?");
            params.add(to);
        }

        return dateFilter.toString().replaceFirst(" AND", " WHERE");
    }

    public SummaryStatsDto getMainDashboard(String from, String to) {
        List<Object> params = new ArrayList<>();

        String filter = createDateFilter(params, from, to);

        String sqlTotal = "SELECT COUNT(*) FROM EventLogSync" + filter;
        String sqlErrors = "SELECT COUNT(*) FROM EventLogSync" + filter + " AND severity >= 2";
        String sqlUsers = "SELECT COUNT(DISTINCT user_id) FROM EventLogSync" + filter;
        String sqlTopErrors = """
                SELECT en.event_human_name as name, COUNT(*) as count
                FROM EventLogSync l
                JOIN EventNames en ON l.event_id = en.event_id
                """ + filter.replace("event_date", "l.event_date") + """
                 AND l.severity >= 2
                GROUP BY en.event_human_name
                ORDER BY count DESC
                """;

        Long total = jdbcTemplate.queryForObject(sqlTotal, Long.class, params.toArray());
        Long errors = jdbcTemplate.queryForObject(sqlErrors, Long.class, params.toArray());
        Long users = jdbcTemplate.queryForObject(sqlUsers, Long.class, params.toArray());

        var topErrorsList = jdbcTemplate.query(sqlTopErrors, (rs, rowNum) -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("name", rs.getString("name"));
            map.put("count", rs.getLong("count"));
            return map;
        }, params.toArray());

        return new SummaryStatsDto(
                total != null ? total : 0L,
                errors != null ? errors : 0L,
                users != null ? users : 0L,
                topErrorsList
        );
    }

    public List<BackgroundTask> getBackgroundTasks(String from, String to){
        List<Object> params = new ArrayList<>();
        String filter = createDateFilter(params, from, to);

        String sql = """
            SELECT 
                MAX(event_human_name) as event_name,
                MIN(event_date) as start_date,
                MAX(event_date) as end_date,
                DATEDIFF(SECOND, MIN(event_date), MAX(event_date)) as duration_sec
            FROM ViewEventLog
            """ + filter + """
            AND event_human_name LIKE ?
            GROUP BY session_id
            HAVING DATEDIFF(SECOND, MIN(event_date), MAX(event_date)) > 0
            ORDER BY start_date DESC
            """;
        
        params.add("%Фоновое задание%");

        return jdbcTemplate.query(sql, (rs, rowNum) -> new BackgroundTask(
                rs.getString("event_name"),
                rs.getString("start_date"),
                rs.getString("end_date"),
                rs.getLong("duration_sec")
        ), params.toArray());
    }
}
