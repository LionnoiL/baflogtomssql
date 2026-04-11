package ua.haponov.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    public Map<String, Object> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) List<Integer> computerIds,
            @RequestParam(required = false) List<Integer> userIds,
            @RequestParam(required = false) List<Integer> appIds,
            @RequestParam(required = false) List<Integer> eventIds,
            @RequestParam(required = false) List<Integer> severityIds) {

        StringBuilder whereClause = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (from != null) {
            whereClause.append(" AND els.event_date >= ? ");
            params.add(from);
        }
        if (to != null) {
            whereClause.append(" AND els.event_date <= ? ");
            params.add(to);
        }

        addInClause(whereClause, params, "els.computer_id", computerIds);
        addInClause(whereClause, params, "els.user_id", userIds);
        addInClause(whereClause, params, "els.app_id", appIds);
        addInClause(whereClause, params, "els.event_id", eventIds);
        addInClause(whereClause, params, "els.severity", severityIds);

        if (search != null && !search.isBlank()) {
            String searchPattern = "%" + search + "%";
            whereClause.append("""
                        AND (els.comment LIKE ? 
                        OR els.data_info LIKE ? 
                        OR u.user_name LIKE ? 
                        OR en.event_human_name LIKE ? 
                        OR c.computer_name LIKE ? 
                        OR app.app_name LIKE ? 
                        OR m.metadata_name LIKE ?)
                    """);
            for (int i = 0; i < 7; i++) params.add(searchPattern);
        }

        String countSql = """
                SELECT COUNT(*) 
                FROM EventLogSync els
                LEFT JOIN Users u ON els.user_id = u.user_id
                LEFT JOIN EventNames en ON els.event_id = en.event_id
                LEFT JOIN Computers c ON els.computer_id = c.computer_id
                LEFT JOIN Applications app ON els.app_id = app.app_id
                LEFT JOIN Metadata m ON els.metadata_id = m.metadata_id
                """ + whereClause;

        Integer totalRows = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());

        String dataSql = """
                SELECT 
                    els.row_id, els.event_date, els.comment, els.data_info,
                    u.user_name, en.event_human_name, c.computer_name, 
                    app.app_name, m.metadata_name, sl.severity_name, sl.severity_color
                FROM EventLogSync els
                LEFT JOIN Users u ON els.user_id = u.user_id
                LEFT JOIN EventNames en ON els.event_id = en.event_id
                LEFT JOIN Computers c ON els.computer_id = c.computer_id
                LEFT JOIN Applications app ON els.app_id = app.app_id
                LEFT JOIN Metadata m ON els.metadata_id = m.metadata_id
                LEFT JOIN SeverityLevels sl ON els.severity = sl.severity_id
                """ + whereClause + """
                ORDER BY els.row_id DESC
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """;

        params.add(page * size);
        params.add(size);

        List<Map<String, Object>> logs = jdbcTemplate.queryForList(dataSql, params.toArray());

        return Map.of(
                "content", logs,
                "totalElements", totalRows != null ? totalRows : 0,
                "page", page,
                "size", size,
                "totalPages", (int) Math.ceil((double) (totalRows != null ? totalRows : 0) / size)
        );
    }

    private void addInClause(StringBuilder where, List<Object> params, String columnName, List<Integer> ids) {
        if (ids != null && !ids.isEmpty()) {
            String placeholders = ids.stream()
                    .map(id -> "?")
                    .collect(Collectors.joining(","));
            where.append(" AND ").append(columnName).append(" IN (").append(placeholders).append(") ");
            params.addAll(ids);
        }
    }
}
