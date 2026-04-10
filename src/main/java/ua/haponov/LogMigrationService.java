package ua.haponov;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LogMigrationService {

    private static final Map<String, String> EVENT_NAME_MAP = Map.ofEntries(
            Map.entry("_$Data$_.New", "Создание"),
            Map.entry("_$Data$_.Update", "Изменение"),
            Map.entry("_$Data$_.Delete", "Удаление"),
            Map.entry("_$Data$_.Post", "Проведение"),
            Map.entry("_$Data$_.Unpost", "Отмена проведения"),
            Map.entry("_$Transaction$_.Begin", "Транзакция. Начало"),
            Map.entry("_$Transaction$_.Commit", "Транзакция. Фиксация"),
            Map.entry("_$Transaction$_.Rollback", "Транзакция. Отмена"),
            Map.entry("_$Session$_.Start", "Сеанс. Начало"),
            Map.entry("_$Session$_.Finish", "Сеанс. Завершение"),
            Map.entry("_$Session$_.Authentication", "Аутентификация"),
            Map.entry("_$Session$_.AuthenticationError", "Ошибка аутентификации"),
            Map.entry("_$Job$_.Start", "Фоновое задание. Запуск"),
            Map.entry("_$Job$_.Succeed", "Фоновое задание. Успешное завершение"),
            Map.entry("_$Job$_.Error", "Фоновое задание. Ошибка выполнения"),
            Map.entry("_$Job$_.Terminate", "Фоновое задание. Отмена"),
            Map.entry("_$User$_.New", "Пользователь. Новый"),
            Map.entry("_$User$_.Update", "Пользователь. Изменение"),
            Map.entry("_$User$_.Delete", "Пользователь. Удаление"),
            Map.entry("_$Config$_.Update", "Изменение конфигурации"),
            Map.entry("_$Metadata$_.Update", "Изменение метаданных"),
            Map.entry("_$PerformError$_", "Ошибка выполнения"),
            Map.entry("_$InfoBase$_.EventLogSettingsUpdateError", "Ошибка настройки журнала регистрации"),
            Map.entry("_$InfoBase$_.DBConfigUpdateError", "Ошибка обновления конфигурации БД")
    );

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
    @Value("${app.migration.delete-after-sync:false}")
    private boolean deleteAfterSync;

    @Scheduled(fixedDelayString = "${app.migration.interval-ms}")
    public void runMigration() {

        log.info("Начало цикла миграции...");

        String selectSql = """
                  SELECT el.rowID, el.date, el.userCode, el.eventCode, el.computerCode, el.appCode,
                      el.comment, el.severity, el.dataPresentation, el.metadataCodes
                  FROM EventLog el
                  WHERE el.rowID > ? ORDER BY el.rowID ASC LIMIT ?
                """;

        String insertSql = """
                INSERT INTO EventLogSync (row_id, event_date, user_id, event_id, computer_id, app_id, metadata_id, severity, comment, data_info)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection sqliteConn = DriverManager.getConnection("jdbc:sqlite:" + sqlitePath);
             Connection mssqlConn = DriverManager.getConnection(mssqlUrl, mssqlUser, mssqlPass)) {

            configureSqlite(sqliteConn);

            mssqlConn.setAutoCommit(false);

            syncMetadata(sqliteConn, mssqlConn);
            syncUsers(sqliteConn, mssqlConn);
            syncApplications(sqliteConn, mssqlConn);
            syncComputers(sqliteConn, mssqlConn);
            syncEventNames(sqliteConn, mssqlConn);

            long lastRowId = getLastId(mssqlConn);

            try (PreparedStatement selectStmt = sqliteConn.prepareStatement(selectSql);
                 PreparedStatement insertStmt = mssqlConn.prepareStatement(insertSql)) {

                selectStmt.setLong(1, lastRowId);
                selectStmt.setInt(2, batchSize);

                try (ResultSet rs = selectStmt.executeQuery()) {

                    long firstId = -1;
                    long lastId = -1;
                    int count = 0;
                    List<Long> syncedIds = new ArrayList<>();

                    while (rs.next()) {

                        long rowId = rs.getLong("rowID");
                        if (count == 0) firstId = rowId;
                        lastId = rowId;

                        insertStmt.setLong(1, rowId);
                        insertStmt.setTimestamp(2, Timestamp.valueOf(convertTicks(rs.getLong("date"))));
                        setOptionalInt(insertStmt, 3, rs.getInt("userCode"));
                        setOptionalInt(insertStmt, 4, rs.getInt("eventCode"));
                        setOptionalInt(insertStmt, 5, rs.getInt("computerCode"));
                        setOptionalInt(insertStmt, 6, rs.getInt("appCode"));
                        setOptionalInt(insertStmt, 7, rs.getInt("metadataCodes"));
                        insertStmt.setInt(8, rs.getInt("severity"));
                        insertStmt.setString(9, rs.getString("comment"));
                        insertStmt.setString(10, rs.getString("dataPresentation"));

                        if (deleteAfterSync) {
                            syncedIds.add(rowId);
                        }

                        insertStmt.addBatch();
                        count++;
                    }

                    if (count > 0) {
                        long startTime = System.currentTimeMillis();

                        insertStmt.executeBatch();
                        mssqlConn.commit();

                        long duration = System.currentTimeMillis() - startTime;
                        log.info("Успешно перенесено {} записей за {} мс (скорость: {} зап/сек).",
                                count,
                                duration,
                                (count * 1000L / Math.max(duration, 1)));

                        if (deleteAfterSync) {
                            sqliteConn.setAutoCommit(false);
                            try (PreparedStatement deleteStmt = sqliteConn.prepareStatement(
                                    "DELETE FROM EventLog WHERE rowID >= ? AND rowID <= ?")) {
                                deleteStmt.setLong(1, firstId);
                                deleteStmt.setLong(2, lastId);

                                int deletedCount = deleteStmt.executeUpdate();
                                sqliteConn.commit();
                                log.info("Очищено {} записей в SQLite диапазоном ID [{} - {}].", deletedCount, firstId, lastId);

                                try (Statement stmt = sqliteConn.createStatement()) {
                                    stmt.execute("PRAGMA incremental_vacuum(100);");
                                }


                            } catch (Exception e) {
                                sqliteConn.rollback();
                                throw e;
                            } finally {
                                sqliteConn.setAutoCommit(true);
                            }
                        }

                    } else {
                        log.info("Новых записей не обнаружено.");
                    }
                } catch (Exception e) {
                    mssqlConn.rollback();
                    throw e;
                } finally {
                    mssqlConn.setAutoCommit(true);
                }
            }
        } catch (SQLException e) {
            log.error("Ошибка SQL при миграции: {}", e.getMessage());
        }
    }

    private void configureSqlite(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("PRAGMA auto_vacuum;");
            if (rs.next() && rs.getInt(1) != 2) { // 2 = INCREMENTAL
                stmt.execute("PRAGMA auto_vacuum = INCREMENTAL;");
                stmt.execute("VACUUM;");
                log.info("SQLite переведен в режим INCREMENTAL auto-vacuum.");
            }
        }
    }

    private void setOptionalInt(PreparedStatement stmt, int index, int value) throws SQLException {
        if (value == 0) {
            stmt.setNull(index, Types.INTEGER);
        } else {
            stmt.setInt(index, value);
        }
    }

    private void syncMetadata(Connection sqliteConn, Connection mssqlConn) throws SQLException {
        String selectSql = "SELECT code, name, uuid FROM MetadataCodes";
        String mergeSql = """
                MERGE INTO Metadata AS target
                USING (SELECT ? AS metadata_id, ? AS metadata_name, ? AS uuid) AS source
                ON (target.metadata_id = source.metadata_id)
                WHEN MATCHED THEN
                    UPDATE SET target.metadata_name = source.metadata_name, target.uuid = source.uuid
                WHEN NOT MATCHED THEN
                    INSERT (metadata_id, metadata_name, uuid)
                    VALUES (source.metadata_id, source.metadata_name, source.uuid);
                """;

        try (PreparedStatement selectStmt = sqliteConn.prepareStatement(selectSql);
             ResultSet rs = selectStmt.executeQuery();
             PreparedStatement mergeStmt = mssqlConn.prepareStatement(mergeSql)) {

            int count = 0;
            while (rs.next()) {
                mergeStmt.setInt(1, rs.getInt("code"));
                mergeStmt.setString(2, cleanQuotes(rs.getString("name")));
                mergeStmt.setString(3, rs.getString("uuid"));
                mergeStmt.addBatch();
                count++;
            }

            if (count > 0) {
                mergeStmt.executeBatch();
                log.info("Синхронизировано метаданных: {}", count);
            }
        }
    }

    private void syncUsers(Connection sqliteConn, Connection mssqlConn) throws SQLException {
        String selectSql = "SELECT code, name, uuid FROM UserCodes";
        String mergeSql = """
                MERGE INTO Users AS target
                USING (SELECT ? AS user_id, ? AS user_name, ? AS uuid) AS source
                ON (target.user_id = source.user_id)
                WHEN MATCHED THEN
                    UPDATE SET target.user_name = source.user_name, target.uuid = source.uuid
                WHEN NOT MATCHED THEN
                    INSERT (user_id, user_name, uuid)
                    VALUES (source.user_id, source.user_name, source.uuid);
                """;

        try (PreparedStatement selectStmt = sqliteConn.prepareStatement(selectSql);
             ResultSet rs = selectStmt.executeQuery();
             PreparedStatement mergeStmt = mssqlConn.prepareStatement(mergeSql)) {

            int count = 0;
            while (rs.next()) {
                mergeStmt.setInt(1, rs.getInt("code"));
                mergeStmt.setString(2, cleanQuotes(rs.getString("name")));
                mergeStmt.setString(3, rs.getString("uuid"));
                mergeStmt.addBatch();
                count++;
            }

            if (count > 0) {
                mergeStmt.executeBatch();
                log.info("Синхронизировано пользователей: {}", count);
            }
        }
    }

    private void syncApplications(Connection sqliteConn, Connection mssqlConn) throws SQLException {
        String selectSql = "SELECT code, name FROM AppCodes";
        String mergeSql = """
                MERGE INTO Applications AS target
                USING (SELECT ? AS app_id, ? AS app_name) AS source
                ON (target.app_id = source.app_id)
                WHEN MATCHED THEN
                    UPDATE SET target.app_name = source.app_name
                WHEN NOT MATCHED THEN
                    INSERT (app_id, app_name)
                    VALUES (source.app_id, source.app_name);
                """;

        try (PreparedStatement selectStmt = sqliteConn.prepareStatement(selectSql);
             ResultSet rs = selectStmt.executeQuery();
             PreparedStatement mergeStmt = mssqlConn.prepareStatement(mergeSql)) {

            int count = 0;
            while (rs.next()) {
                int code = rs.getInt("code");
                String name = cleanQuotes(rs.getString("name"));

                if (name == null || name.trim().isEmpty()) {
                    name = "Unknown App (" + code + ")";
                }

                mergeStmt.setInt(1, code);
                mergeStmt.setString(2, name);
                mergeStmt.addBatch();
                count++;
            }

            if (count > 0) {
                mergeStmt.executeBatch();
                log.info("Синхронизировано приложений: {}", count);
            }
        }
    }

    private void syncComputers(Connection sqliteConn, Connection mssqlConn) throws SQLException {
        String selectSql = "SELECT code, name FROM ComputerCodes";
        String mergeSql = """
                MERGE INTO Computers AS target
                USING (SELECT ? AS computer_id, ? AS computer_name) AS source
                ON (target.computer_id = source.computer_id)
                WHEN MATCHED THEN
                    UPDATE SET target.computer_name = source.computer_name
                WHEN NOT MATCHED THEN
                    INSERT (computer_id, computer_name)
                    VALUES (source.computer_id, source.computer_name);
                """;

        try (PreparedStatement selectStmt = sqliteConn.prepareStatement(selectSql);
             ResultSet rs = selectStmt.executeQuery();
             PreparedStatement mergeStmt = mssqlConn.prepareStatement(mergeSql)) {

            int count = 0;
            while (rs.next()) {
                int code = rs.getInt("code");
                String name = cleanQuotes(rs.getString("name"));

                if (name == null || name.trim().isEmpty()) {
                    name = "Unknown Computer (" + code + ")";
                }

                mergeStmt.setInt(1, code);
                mergeStmt.setString(2, name);
                mergeStmt.addBatch();
                count++;
            }

            if (count > 0) {
                mergeStmt.executeBatch();
                log.info("Синхронизировано компьютеров: {}", count);
            }
        }
    }

    private void syncEventNames(Connection sqliteConn, Connection mssqlConn) throws SQLException {
        String selectSql = "SELECT code, name FROM EventCodes";
        String mergeSql = """
                MERGE INTO EventNames AS target
                USING (SELECT ? AS event_id, ? AS event_code, ? AS event_human_name) AS source
                ON (target.event_id = source.event_id)
                WHEN MATCHED AND (target.event_human_name IS NULL OR target.event_human_name = '') THEN
                    UPDATE SET target.event_code = source.event_code, 
                               target.event_human_name = source.event_human_name
                WHEN NOT MATCHED THEN
                    INSERT (event_id, event_code, event_human_name)
                    VALUES (source.event_id, source.event_code, source.event_human_name);
                """;

        try (PreparedStatement selectStmt = sqliteConn.prepareStatement(selectSql);
             ResultSet rs = selectStmt.executeQuery();
             PreparedStatement mergeStmt = mssqlConn.prepareStatement(mergeSql)) {

            int count = 0;
            while (rs.next()) {
                int codeId = rs.getInt("code");
                String codeStr = cleanQuotes(rs.getString("name"));

                if (codeStr != null && !codeStr.trim().isEmpty()) {
                    String humanName = EVENT_NAME_MAP.getOrDefault(codeStr, codeStr);

                    mergeStmt.setInt(1, codeId);
                    mergeStmt.setString(2, codeStr);
                    mergeStmt.setString(3, humanName);
                    mergeStmt.addBatch();
                    count++;
                }
            }

            if (count > 0) {
                mergeStmt.executeBatch();
                log.info("Синхронизировано кодов событий: {}", count);
            }
        }
    }

    private String cleanQuotes(String value) {
        if (value == null) return "Unknown";
        return value.replace("\"", "").trim();
    }

    private LocalDateTime convertTicks(long ticks) {
        long seconds = (ticks / 10000) - TICKS_AT_EPOCH;
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds), ZoneId.systemDefault());
    }

    private long getLastId(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT ISNULL(MAX(row_id), 0) FROM EventLogSync");
            return rs.next() ? rs.getLong(1) : 0;
        }
    }
}