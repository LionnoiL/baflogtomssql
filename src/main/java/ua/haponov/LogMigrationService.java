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
            // --- ДОСТУП ---
            Map.entry("_$Access$_.Access", "Доступ. Доступ"),
            Map.entry("_$Access$_.AccessDenied", "Доступ. Отказ в доступе"),

            // --- ДАННЫЕ (Data) ---
            Map.entry("_$Data$_.New", "Данные. Добавление"),
            Map.entry("_$Data$_.Update", "Данные. Изменение"),
            Map.entry("_$Data$_.Delete", "Данные. Удаление"),
            Map.entry("_$Data$_.Post", "Данные. Проведение"),
            Map.entry("_$Data$_.Unpost", "Данные. Отмена проведения"),
            Map.entry("_$Data$_.NewPredefinedData", "Данные. Добавление предопределенных данных"),
            Map.entry("_$Data$_.UpdatePredefinedData", "Данные. Изменение предопределенных данных"),
            Map.entry("_$Data$_.DeletePredefinedData", "Данные. Удаление предопределенных данных"),
            Map.entry("_$Data$_.NewVersion", "Данные. Добавление версии"),
            Map.entry("_$Data$_.DeleteVersions", "Данные. Удаление версий"),
            Map.entry("_$Data$_.VersionCommentUpdate", "Данные. Изменение комментария версии"),
            Map.entry("_$Data$_.PredefinedDataInitialization", "Данные. Инициализация предопределенных данных"),
            Map.entry("_$Data$_.PredefinedDataInitializationDataNotFound", "Данные. Инициализация предопределенных данных (не найдены)"),
            Map.entry("_$Data$_.SetPredefinedDataInitialization", "Данные. Установка инициализации предопределенных данных"),
            Map.entry("_$Data$_.SetStandardODataInterfaceContent", "Данные. Изменение состава OData"),
            Map.entry("_$Data$_.TotalsMaxPeriodUpdate", "Данные. Изменение макс. периода итогов"),
            Map.entry("_$Data$_.TotalsMinPeriodUpdate", "Данные. Изменение мин. периода итогов"),

            // --- ИНФОРМАЦИОННАЯ БАЗА (InfoBase) ---
            Map.entry("_$InfoBase$_.ConfigUpdate", "ИБ. Изменение конфигурации"),
            Map.entry("_$InfoBase$_.DBConfigUpdate", "ИБ. Изменение конфигурации БД"),
            Map.entry("_$InfoBase$_.DBConfigUpdateStart", "ИБ. Запуск обновления конфигурации БД"),
            Map.entry("_$InfoBase$_.DBConfigUpdateError", "ИБ. Ошибка обновления конфигурации БД"),
            Map.entry("_$InfoBase$_.ConfigExtensionUpdate", "ИБ. Изменение расширения конфигурации"),
            Map.entry("_$InfoBase$_.DBConfigExtensionUpdate", "ИБ. Изменение расширения в БД"),
            Map.entry("_$InfoBase$_.DBConfigExtensionUpdateError", "ИБ. Ошибка изменения расширения в БД"),
            Map.entry("_$InfoBase$_.DBConfigBackgroundUpdateStart", "ИБ. Запуск фонового обновления БД"),
            Map.entry("_$InfoBase$_.DBConfigBackgroundUpdateFinish", "ИБ. Завершение фонового обновления БД"),
            Map.entry("_$InfoBase$_.DBConfigBackgroundUpdateCancel", "ИБ. Отмена фонового обновления БД"),
            Map.entry("_$InfoBase$_.DBConfigBackgroundUpdateResume", "ИБ. Продолжение фонового обновления БД"),
            Map.entry("_$InfoBase$_.DBConfigBackgroundUpdateSuspend", "ИБ. Пауза фонового обновления БД"),
            Map.entry("_$InfoBase$_.EventLogSettingsUpdate", "ИБ. Изменение параметров журнала регистрации"),
            Map.entry("_$InfoBase$_.EventLogSettingsUpdateError", "ИБ. Ошибка настройки журнала регистрации"),
            Map.entry("_$InfoBase$_.EventLogReduce", "ИБ. Сокращение журнала регистрации"),
            Map.entry("_$InfoBase$_.EventLogReduceError", "ИБ. Ошибка сокращения журнала регистрации"),
            Map.entry("_$InfoBase$_.DumpStart", "ИБ. Начало выгрузки в файл"),
            Map.entry("_$InfoBase$_.DumpFinish", "ИБ. Окончание выгрузки в файл"),
            Map.entry("_$InfoBase$_.DumpError", "ИБ. Ошибка выгрузки в файл"),
            Map.entry("_$InfoBase$_.RestoreStart", "ИБ. Начало загрузки из файла"),
            Map.entry("_$InfoBase$_.RestoreFinish", "ИБ. Окончание загрузки из файла"),
            Map.entry("_$InfoBase$_.RestoreError", "ИБ. Ошибка загрузки из файла"),
            Map.entry("_$InfoBase$_.ParametersUpdate", "ИБ. Изменение параметров ИБ"),
            Map.entry("_$InfoBase$_.ParametersUpdateError", "ИБ. Ошибка изменения параметров ИБ"),
            Map.entry("_$InfoBase$_.MasterNodeUpdate", "ИБ. Изменение главного узла"),
            Map.entry("_$InfoBase$_.PredefinedDataUpdate", "ИБ. Обновление предопределенных данных"),
            Map.entry("_$InfoBase$_.RegionalSettingsUpdate", "ИБ. Изменение региональных установок"),
            Map.entry("_$InfoBase$_.EraseData", "ИБ. Удаление данных ИБ"),
            Map.entry("_$InfoBase$_.AdditionalAuthenticationSettingsUpdate", "ИБ. Изменение настроек аутентификации"),
            Map.entry("_$InfoBase$_.SecondAuthenticationFactorTemplateNew", "ИБ. Добавление шаблона 2-го фактора"),
            Map.entry("_$InfoBase$_.SecondAuthenticationFactorTemplateUpdate", "ИБ. Изменение шаблона 2-го фактора"),
            Map.entry("_$InfoBase$_.SecondAuthenticationFactorTemplateDelete", "ИБ. Удаление шаблона 2-го фактора"),
            Map.entry("_$InfoBase$_.IntegrationServiceActiveUpdate", "ИБ. Изменение активности сервиса интеграции"),
            Map.entry("_$InfoBase$_.IntegrationServiceSettingsUpdate", "ИБ. Изменение настроек сервиса интеграции"),

            // --- ТЕСТИРОВАНИЕ И ИСПРАВЛЕНИЕ ---
            Map.entry("_$InfoBase$_.VerifyAndRepairInfo", "Тестирование и исправление. Сообщение"),
            Map.entry("_$InfoBase$_.VerifyAndRepairMessage", "Тестирование и исправление. Предупреждение"),
            Map.entry("_$InfoBase$_.VerifyAndRepairImportant", "Тестирование и исправление. Ошибка"),

            // --- ФОНОВЫЕ ЗАДАНИЯ (Jobs) ---
            Map.entry("_$Job$_.Start", "Фоновое задание. Запуск"),
            Map.entry("_$Job$_.Finish", "Фоновое задание. Завершение."),
            Map.entry("_$Job$_.Succeed", "Фоновое задание. Успешное завершение"),
            Map.entry("_$Job$_.Fail", "Фоновое задание. Ошибка выполнения"),
            Map.entry("_$Job$_.Error", "Фоновое задание. Ошибка (системная)"),
            Map.entry("_$Job$_.Cancel", "Фоновое задание. Отмена"),
            Map.entry("_$Job$_.Terminate", "Фоновое задание. Принудительное завершение"),

            // --- СЕАНСЫ (Sessions) ---
            Map.entry("_$Session$_.Start", "Сеанс. Начало"),
            Map.entry("_$Session$_.Finish", "Сеанс. Завершение"),
            Map.entry("_$Session$_.Authentication", "Сеанс. Аутентификация"),
            Map.entry("_$Session$_.AuthenticationError", "Сеанс. Ошибка аутентификации"),
            Map.entry("_$Session$_.AuthenticationFirstFactor", "Сеанс. Аутентификация (1-й фактор)"),
            Map.entry("_$Session$_.ConfigExtensionApplyError", "Сеанс. Ошибка применения расширения"),

            // --- ТРАНЗАКЦИИ ---
            Map.entry("_$Transaction$_.Begin", "Транзакция. Начало"),
            Map.entry("_$Transaction$_.Commit", "Транзакция. Фиксация"),
            Map.entry("_$Transaction$_.Rollback", "Транзакция. Отмена"),

            // --- ПОЛЬЗОВАТЕЛИ ---
            Map.entry("_$User$_.New", "Пользователи. Добавление"),
            Map.entry("_$User$_.Update", "Пользователи. Изменение"),
            Map.entry("_$User$_.Delete", "Пользователи. Удаление"),
            Map.entry("_$User$_.NewError", "Пользователи. Ошибка добавления"),
            Map.entry("_$User$_.UpdateError", "Пользователи. Ошибка изменения"),
            Map.entry("_$User$_.DeleteError", "Пользователи. Ошибка удаления"),
            Map.entry("_$User$_.AuthenticationLock", "Пользователи. Блокировка аутентификации"),
            Map.entry("_$User$_.AuthenticationUnlock", "Пользователи. Разблокировка аутентификации"),
            Map.entry("_$User$_.AuthenticationUnlockError", "Пользователи. Ошибка разблокировки"),

            // --- ПРОЧЕЕ ---
            Map.entry("_$PerformError$_", "Ошибка выполнения"),
            Map.entry("_$OpenIDProvider$_.PositiveAssertion", "OpenID. Подтверждено"),
            Map.entry("_$OpenIDProvider$_.NegativeAssertion", "OpenID. Отклонено")
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