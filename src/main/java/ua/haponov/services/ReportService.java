package ua.haponov.services;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ua.haponov.dto.reports.BackgroundTask;
import ua.haponov.dto.reports.CurrentUser;
import ua.haponov.dto.reports.SummaryStatsDto;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private final JdbcTemplate jdbcTemplate;
    private final SettingsService settingsService;

    private String createDateFilter(List<Object> params, String from, String to) {
        StringBuilder dateFilter = new StringBuilder();
        if (from != null && !from.isEmpty()) {
            dateFilter.append(" AND event_date >= ?");
            params.add(from);
        } else {
            dateFilter.append(" AND event_date >= CAST(GETDATE() AS DATE)");
        }

        if (to != null && !to.isEmpty()) {
            dateFilter.append(" AND event_date <= ?");
            params.add(to + " 23:59:59");
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

    public List<BackgroundTask> getBackgroundTasks(String from, String to) {
        List<Object> params = new ArrayList<>();
        String filter = createDateFilter(params, from, to);

        String sql = """
                SELECT
                                 session_id,
                                 MAX(metadata_name) as metadata_name,
                                 MAX(CASE
                                     WHEN event_code = '_$Job$_.Succeed' THEN 'Успешно'
                                     WHEN event_code = '_$Job$_.Fail' THEN 'Ошибка выполнения'
                                     WHEN event_code = '_$Job$_.Error' THEN 'Системная ошибка'
                                     WHEN event_code = '_$Job$_.Cancel' THEN 'Отменено'
                                     WHEN event_code = '_$Job$_.Terminate' THEN 'Прервано'
                                     ELSE 'Завершено'
                                 END) as status,
                                 MAX(CASE WHEN event_code = '_$Job$_.Start' THEN event_date END) as start_date,
                                 MAX(CASE WHEN event_code <> '_$Job$_.Start' THEN event_date END) as end_date,
                                 DATEDIFF(SECOND,
                                     MAX(CASE WHEN event_code = '_$Job$_.Start' THEN event_date END),
                                     MAX(CASE WHEN event_code <> '_$Job$_.Start' THEN event_date END)
                                 ) as duration_sec
                             FROM ViewEventLog
                """ + filter + """
                                      AND event_code IN (
                                      '_$Job$_.Start',
                                      '_$Job$_.Finish',
                                      '_$Job$_.Succeed',
                                      '_$Job$_.Fail',
                                      '_$Job$_.Error',
                                      '_$Job$_.Cancel',
                                      '_$Job$_.Terminate'
                                  )
                
                                 GROUP BY session_id
                                 HAVING MAX(CASE WHEN event_code = '_$Job$_.Start' THEN 1 ELSE 0 END) = 1
                                 ORDER BY start_date DESC;
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new BackgroundTask(
                rs.getString("metadata_name"),
                rs.getString("start_date"),
                rs.getString("end_date"),
                rs.getLong("duration_sec")
        ), params.toArray());
    }

    public List<CurrentUser> getCurrentUsers() {

        String sql = """
                SELECT
                    user_name,
                    user_uuid,
                    session_id,
                    MIN(event_date) AS session_start_time
                FROM ViewEventLog
                WHERE event_date >= CAST(GETDATE() AS DATE)
                  AND event_code IN ('_$Session$_.Start', '_$Session$_.Finish')
                GROUP BY
                    user_name,
                    user_uuid,
                    session_id
                HAVING
                    COUNT(CASE WHEN event_code = '_$Session$_.Start' THEN 1 END) > 0
                    AND COUNT(CASE WHEN event_code = '_$Session$_.Finish' THEN 1 END) = 0
                ORDER BY
                    session_start_time ASC;
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            var startTime = rs.getTimestamp("session_start_time");
            String formattedDate = startTime != null
                    ? startTime.toLocalDateTime().format(DATE_FORMATTER)
                    : "";

            return new CurrentUser(
                    rs.getString("user_name"),
                    rs.getString("user_uuid"),
                    formattedDate,
                    rs.getInt("session_id")
            );
        });
    }
}
