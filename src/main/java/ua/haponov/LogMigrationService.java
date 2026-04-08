package ua.haponov;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
public class LogMigrationService {

    private static final long TICKS_AT_EPOCH = 62135596800L;
    @Value("${app.sqlite.path}")
    private String sqlitePath;
    @Value("${app.mssql.url}")
    private String mssqlUrl;
    @Value("${app.mssql.username}")
    private String mssqlUser;
    @Value("${app.mssql.password}")
    private String mssqlPass;
    @Value("${app.migration.batch-size}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.migration.interval-ms}")
    public void runMigration() {
        log.info("Начало цикла миграции...");

        String selectSql = """
                SELECT el.RowID, el.Date, u.Name as User, e.Name as Event, c.Name as Computer, 
                       el.Comment, el.Severity, el.DataPresentation
                FROM EventLog el
                LEFT JOIN UserCodes u ON el.UserCode = u.Code
                LEFT JOIN EventCodes e ON el.EventCode = e.Code  -- <--- Проверьте здесь
                LEFT JOIN ComputerCodes c ON el.ComputerCode = c.Code
                WHERE el.RowID > ? ORDER BY el.RowID ASC LIMIT ?
                """;

        String insertSql = """
                INSERT INTO EventLogSync (row_id, event_date, user_name, event_name, computer, comment, severity, data_info) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection sqliteConn = DriverManager.getConnection("jdbc:sqlite:" + sqlitePath);
             Connection mssqlConn = DriverManager.getConnection(mssqlUrl, mssqlUser, mssqlPass)) {

            long lastRowId = checkAndGetLastId(mssqlConn);

            try (PreparedStatement selectStmt = sqliteConn.prepareStatement(selectSql);
                 PreparedStatement insertStmt = mssqlConn.prepareStatement(insertSql)) {

                selectStmt.setLong(1, lastRowId);
                selectStmt.setInt(2, batchSize);

                ResultSet rs = selectStmt.executeQuery();
                int count = 0;

                while (rs.next()) {
                    insertStmt.setLong(1, rs.getLong("RowID"));
                    insertStmt.setTimestamp(2, Timestamp.valueOf(convertTicks(rs.getLong("Date"))));
                    insertStmt.setString(3, rs.getString("User"));
                    insertStmt.setString(4, rs.getString("Event"));
                    insertStmt.setString(5, rs.getString("Computer"));
                    insertStmt.setString(6, rs.getString("Comment"));
                    insertStmt.setInt(7, rs.getInt("Severity"));
                    insertStmt.setString(8, rs.getString("DataPresentation"));
                    insertStmt.addBatch();
                    count++;
                }

                if (count > 0) {
                    insertStmt.executeBatch();
                    log.info("Успешно перенесено {} записей.", count);
                } else {
                    log.info("Новых записей не обнаружено.");
                }
            }
        } catch (SQLException e) {
            log.error("Ошибка SQL при миграции: {}", e.getMessage());
        }
    }

    private LocalDateTime convertTicks(long ticks) {
        long seconds = (ticks / 10000) - TICKS_AT_EPOCH;
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds), ZoneId.systemDefault());
    }

    private long checkAndGetLastId(Connection conn) throws SQLException {
        String tableCheck = """
                IF OBJECT_ID('EventLogSync', 'U') IS NULL 
                CREATE TABLE EventLogSync (
                    row_id BIGINT PRIMARY KEY, 
                    event_date DATETIME, 
                    user_name NVARCHAR(255), 
                    event_name NVARCHAR(255), 
                    computer NVARCHAR(255), 
                    comment NVARCHAR(MAX), 
                    severity INT, 
                    data_info NVARCHAR(MAX)
                );
                """;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(tableCheck);
            ResultSet rs = stmt.executeQuery("SELECT ISNULL(MAX(row_id), 0) FROM EventLogSync");
            return rs.next() ? rs.getLong(1) : 0;
        }
    }
}