package ua.haponov.services;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ua.haponov.dto.reports.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private final JdbcTemplate jdbcTemplate;
    private final SettingsService settingsService;
    private final DictionaryService dictionaryService;
    private final MessageSource messageSource;

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

    public List<BackgroundTaskDto> getBackgroundTasks(String from, String to) {
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
                                 ORDER BY start_date ASC;
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new BackgroundTaskDto(
                rs.getString("metadata_name"),
                rs.getString("start_date"),
                rs.getString("end_date"),
                rs.getLong("duration_sec")
        ), params.toArray());
    }

    public List<CurrentUserDto> getCurrentUsers() {

        String sql = """
                 WITH LastEvents AS (
                    SELECT
                        user_name,
                        user_uuid,
                        session_id,
                        event_code,
                        event_date,
                        ROW_NUMBER() OVER (
                            PARTITION BY user_uuid, session_id\s
                            ORDER BY event_date DESC
                        ) as rn
                    FROM ViewEventLog
                    WHERE event_date >= CAST(GETDATE() AS DATE)
                      AND event_code IN ('_$Session$_.Start', '_$Session$_.Finish')
                )
                SELECT
                    user_name,
                    user_uuid,
                    session_id,
                    event_date AS session_start_time
                FROM LastEvents
                WHERE rn = 1
                  AND event_code = '_$Session$_.Start'
                ORDER BY event_date;
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            var startTime = rs.getTimestamp("session_start_time");
            String formattedDate = startTime != null
                    ? startTime.toLocalDateTime().format(DATE_FORMATTER)
                    : "";

            return new CurrentUserDto(
                    rs.getString("user_name"),
                    rs.getString("user_uuid"),
                    formattedDate,
                    rs.getInt("session_id")
            );
        });
    }

    public List<SuspicionDto> getSuspicionsReasons(String from, String to) {
        List<Integer> weekendDays = settingsService.getWeekendDays();
        String nightStart = settingsService.getNightStartTime();
        String nightEnd = settingsService.getNightEndTime();

        boolean hasWeekends = weekendDays != null && !weekendDays.isEmpty();
        boolean hasNight = nightStart != null && !nightStart.isEmpty() && nightEnd != null && !nightEnd.isEmpty();

        if (!hasWeekends && !hasNight) {
            return new ArrayList<>();
        }

        List<Object> params = new ArrayList<>();
        String dateFilter = createDateFilter(params, from, to);

        String weekendLabel = messageSource.getMessage("reports.suspicion.weekend", null, "Выходной день", LocaleContextHolder.getLocale());
        String nightLabel = messageSource.getMessage("reports.suspicion.night", null, "Ночное время", LocaleContextHolder.getLocale());
        String otherLabel = messageSource.getMessage("reports.suspicion.other", null, "Другое", LocaleContextHolder.getLocale());

        StringBuilder caseBuilder = new StringBuilder("CASE ");
        List<Object> caseParams = new ArrayList<>();

        if (hasWeekends) {
            String daysPlaceholder = weekendDays.stream().map(d -> "?").collect(java.util.stream.Collectors.joining(","));
            caseBuilder.append("WHEN event_dw IN (").append(daysPlaceholder).append(") THEN ? ");
            caseParams.addAll(weekendDays);
            caseParams.add(weekendLabel);
        }

        if (hasNight) {
            caseBuilder.append("WHEN CAST(event_date AS TIME) >= ? OR CAST(event_date AS TIME) <= ? THEN ? ");
            caseParams.add(nightStart);
            caseParams.add(nightEnd);
            caseParams.add(nightLabel);
        }
        caseBuilder.append("ELSE ? END");
        caseParams.add(otherLabel);

        StringBuilder suspicionFilter = new StringBuilder(" AND (");
        List<Object> filterParams = new ArrayList<>();

        if (hasWeekends) {
            String daysPlaceholder = weekendDays.stream().map(d -> "?").collect(java.util.stream.Collectors.joining(","));
            suspicionFilter.append("event_dw IN (").append(daysPlaceholder).append(")");
            filterParams.addAll(weekendDays);
        }

        if (hasNight) {
            if (hasWeekends) suspicionFilter.append(" OR ");
            suspicionFilter.append("(CAST(event_date AS TIME) >= ? OR CAST(event_date AS TIME) <= ?)");
            filterParams.add(nightStart);
            filterParams.add(nightEnd);
        }
        suspicionFilter.append(")");

        String sql = "SELECT user_name, event_human_name, app_name, computer_name, event_date, " +
                caseBuilder + " AS suspicion_reason, " +
                "severity_name, comment, data_info FROM ViewEventLog " +
                dateFilter + suspicionFilter +
                " AND app_name NOT IN ('BackgroundJob') ORDER BY event_date DESC";

        List<Object> finalParams = new ArrayList<>();
        finalParams.addAll(caseParams);
        finalParams.addAll(params);
        finalParams.addAll(filterParams);

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            var eventDate = rs.getTimestamp("event_date");
            String formattedDate = eventDate != null
                    ? eventDate.toLocalDateTime().format(DATE_FORMATTER)
                    : "";

            return new SuspicionDto(
                    rs.getString("user_name"),
                    rs.getString("event_human_name"),
                    rs.getString("app_name"),
                    rs.getString("computer_name"),
                    formattedDate,
                    rs.getString("suspicion_reason"),
                    rs.getString("comment"),
                    rs.getString("data_info")
            );
        }, finalParams.toArray());
    }

    public ChartsLoadDto getChartsLoad(String from, String to) {
        return new ChartsLoadDto(
                getLoadGraph(from, to),
                getIntraDiurnalActivity(from, to)
        );
    }

    public List<LoadStatsDto> getLoadGraph(String from, String to) {
        List<Object> params = new ArrayList<>();
        String filter = createDateFilter(params, from, to);

        String sql = """
                WITH HourlyStats AS (
                    SELECT
                        event_hour AS [Hour],
                        COUNT(*) AS EventCount
                    FROM EventLogSync
                """ + filter + """
                    GROUP BY event_hour
                )
                SELECT
                    [Hour],
                    EventCount,
                    CAST(EventCount * 100.0 / SUM(EventCount) OVER() AS DECIMAL(5, 2)) AS LoadPercentage
                FROM HourlyStats
                ORDER BY [Hour];
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new LoadStatsDto(
                rs.getInt("Hour"),
                rs.getLong("EventCount"),
                rs.getBigDecimal("LoadPercentage")
        ), params.toArray());
    }

    public List<IntraDiurnalActivityDto> getIntraDiurnalActivity(String from, String to) {
        List<Object> params = new ArrayList<>();
        String filter = createDateFilter(params, from, to);

        String sql = """
                SELECT
                    CAST(event_date AS DATE) AS [Date],
                    COUNT(*) AS EventCount,
                    COUNT(DISTINCT user_id) AS UniqueUsers,
                    COUNT(DISTINCT session_id) AS SessionsCount
                FROM EventLogSync
                """ + filter + """
                GROUP BY CAST(event_date AS DATE)
                ORDER BY [Date] DESC;
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new IntraDiurnalActivityDto(
                rs.getString("Date"),
                rs.getInt("EventCount"),
                rs.getInt("UniqueUsers"),
                rs.getInt("SessionsCount")
        ), params.toArray());
    }

    public List<ActivityMatrixDto> getActivityMatrix(String from, String to) {

        List<Object> params = new ArrayList<>();
        String filter = createDateFilter(params, from, to);

        String sql = """
                SELECT
                    event_hour AS [Hour],
                    COUNT(CASE WHEN event_dw = 1 THEN 1 END) AS [Mon],
                    COUNT(CASE WHEN event_dw= 2 THEN 1 END) AS [Tue],
                    COUNT(CASE WHEN event_dw = 3 THEN 1 END) AS [Wed],
                    COUNT(CASE WHEN event_dw= 4 THEN 1 END) AS [Thu],
                    COUNT(CASE WHEN event_dw = 5 THEN 1 END) AS [Fri],
                    COUNT(CASE WHEN event_dw = 6 THEN 1 END) AS [Sat],
                    COUNT(CASE WHEN event_dw = 7 THEN 1 END) AS [Sun],
                    COUNT(*) AS TotalPerHour
                FROM EventLogSync
                """ + filter + """
                GROUP BY event_hour
                ORDER BY [Hour];
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new ActivityMatrixDto(
                rs.getInt("Hour"),
                rs.getLong("Mon"),
                rs.getLong("Tue"),
                rs.getLong("Wed"),
                rs.getLong("Thu"),
                rs.getLong("Fri"),
                rs.getLong("Sat"),
                rs.getLong("Sun"),
                rs.getLong("TotalPerHour")
        ), params.toArray());
    }

    public List<AuthAuditDto> getAuthorizationAudit(String from, String to) {
        List<Object> params = new ArrayList<>();
        String filter = createDateFilter(params, from, to);

        List<Integer> eventIds = dictionaryService.getSessionEventIds();

        List<Integer> safeIds = eventIds.isEmpty() ? List.of(-1) : eventIds;
        safeIds.forEach(params::add);

        String placeholders = safeIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(", "));
        String sql = """
                SELECT
                      l.event_date,
                      l.user_name,
                      l.computer_name,
                      l.app_name,
                      CASE
                          WHEN l.event_code = '_$Session$_.Start' THEN N'Успішний вхід'
                          WHEN l.event_code = '_$Session$_.AuthenticationError' THEN N'Помилка автентифікації'
                          WHEN l.event_code = '_$Session$_.Authentication' THEN N'Автентифікація'
                          ELSE N'Інше'
                      END AS auth_status,
                      l.severity_name,
                      l.comment,
                  	l.data_info,
                  	l.data
                  FROM ViewEventLog l
                """ + filter +
                " AND  l.event_id IN (" + placeholders + ")" +
                """
                  	AND l.app_name != 'HTTPServiceConnection'
                  ORDER BY
                    l.event_date ASC;
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            var eventDate = rs.getTimestamp("event_date");
            String formattedDate = eventDate != null
                    ? eventDate.toLocalDateTime().format(DATE_FORMATTER)
                    : "";

            return new AuthAuditDto(
                    formattedDate,
                    rs.getString("user_name"),
                    rs.getString("computer_name"),
                    rs.getString("app_name"),
                    rs.getString("auth_status"),
                    rs.getString("severity_name"),
                    rs.getString("comment"),
                    DataInfoDecoder.extractValue(rs.getString("data"))
            );
        }, params.toArray());
    }

    public List<AdministrativeActionDto> getAdministrativeActions(String from, String to) {
        List<Object> params = new ArrayList<>();
        String filter = createDateFilter(params, from, to);

        List<Integer> eventIds = dictionaryService.getInfoBaseEventIds();

        List<Integer> safeIds = eventIds.isEmpty() ? List.of(-1) : eventIds;
        safeIds.forEach(params::add);

        String placeholders = safeIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(", "));

        String sql = """
                SELECT
                    l.event_date,
                    u.user_name,
                    c.computer_name,
                    en.event_human_name,
                    a.app_name,
                    l.comment,
                    l.data_info
                FROM EventLogSync l
                INNER JOIN Users u        ON l.user_id = u.user_id
                INNER JOIN EventNames en  ON l.event_id = en.event_id
                LEFT  JOIN Computers c    ON l.computer_id = c.computer_id
                LEFT  JOIN Applications a ON l.app_id = a.app_id
                """
                + filter.replace("event_date", "l.event_date")
                + " AND l.event_id IN (" + placeholders + ")"
                + " ORDER BY l.event_date ASC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            var eventDate = rs.getTimestamp("event_date");
            String formattedDate = eventDate != null
                    ? eventDate.toLocalDateTime().format(DATE_FORMATTER)
                    : "";

            return new AdministrativeActionDto(
                    formattedDate,
                    rs.getString("user_name"),
                    rs.getString("computer_name"),
                    rs.getString("event_human_name"),
                    rs.getString("app_name"),
                    rs.getString("comment"),
                    rs.getString("data_info")
            );
        }, params.toArray());
    }
}
