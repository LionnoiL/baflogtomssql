package ua.haponov.services;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ua.haponov.dto.Log;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LogService {

    private final JdbcTemplate jdbcTemplate;

    public Map<String, Object> getLogsPaged(
            int page, int size, String guid, String search, LocalDateTime from, LocalDateTime to,
            List<Integer> computerIds, List<Integer> userIds, List<Integer> appIds,
            List<Integer> eventIds, List<Integer> severityIds, List<Integer> metadataIds) {

        StringBuilder whereClause = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        buildWhereClause(whereClause, params, guid, search, from, to, computerIds, userIds, appIds, eventIds, severityIds, metadataIds);

        String countSql = "SELECT COUNT(*) FROM EventLogSync els " + getJoins() + whereClause;
        Integer totalRows = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());

        String dataSql = "SELECT els.row_id as id, els.event_date as timestamp, els.comment as message, els.data_info as dataInfo, " +
                "u.user_name as userName, en.event_human_name as eventName, c.computer_name as computerName, " +
                "app.app_name as applicationName, m.metadata_name as metadataName, sl.severity_name as severityName, sl.severity_color as severityColor " +
                "FROM EventLogSync els " + getJoins() + whereClause +
                " ORDER BY els.row_id OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        params.add(page * size);
        params.add(size);

        List<Log> logs = jdbcTemplate.query(dataSql, params.toArray(), new BeanPropertyRowMapper<>(Log.class));

        return Map.of(
                "content", logs,
                "totalElements", totalRows,
                "page", page,
                "size", size,
                "totalPages", (int) Math.ceil((double) totalRows / size)
        );
    }

    private String getJoins() {
        return """
                LEFT JOIN Users u ON els.user_id = u.user_id
                LEFT JOIN EventNames en ON els.event_id = en.event_id
                LEFT JOIN Computers c ON els.computer_id = c.computer_id
                LEFT JOIN Applications app ON els.app_id = app.app_id
                LEFT JOIN Metadata m ON els.metadata_id = m.metadata_id
                LEFT JOIN SeverityLevels sl ON els.severity = sl.severity_id
                """;
    }

    private void buildWhereClause(StringBuilder where, List<Object> params, String guid, String search,
                                  LocalDateTime from, LocalDateTime to,
                                  List<Integer> computerIds, List<Integer> userIds,
                                  List<Integer> appIds, List<Integer> eventIds,
                                  List<Integer> severityIds, List<Integer> metadataIds) {

        if (guid != null && !guid.isBlank()) {
            where.append(" AND els.data = ? ");
            params.add(guid);
        }
        if (from != null) {
            where.append(" AND els.event_date >= ? ");
            params.add(from);
        }
        if (to != null) {
            where.append(" AND els.event_date <= ? ");
            params.add(to);
        }

        addInClause(where, params, "els.computer_id", computerIds);
        addInClause(where, params, "els.user_id", userIds);
        addInClause(where, params, "els.app_id", appIds);
        addInClause(where, params, "els.event_id", eventIds);
        addInClause(where, params, "els.severity", severityIds);
        addInClause(where, params, "els.metadata_id", metadataIds);

        if (search != null && !search.isBlank()) {
            String pattern = "%" + search + "%";
            where.append("""
                    AND (els.search_content LIKE ?)
                    """);
            params.add(pattern);
        }
    }

    private void addInClause(StringBuilder where, List<Object> params, String column, List<Integer> ids) {
        if (ids != null && !ids.isEmpty()) {
            String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(","));
            where.append(" AND ").append(column).append(" IN (").append(placeholders).append(") ");
            params.addAll(ids);
        }
    }
}