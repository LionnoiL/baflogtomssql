-- Работа с данными
IF NOT EXISTS (SELECT 1 FROM EventNames WHERE event_code = '_$Data$_.New')
    INSERT INTO EventNames (event_code, event_human_name) VALUES ('_$Data$_.New', 'Создание');

IF NOT EXISTS (SELECT 1 FROM EventNames WHERE event_code = '_$Data$_.Update')
    INSERT INTO EventNames (event_code, event_human_name) VALUES ('_$Data$_.Update', 'Изменение');

IF NOT EXISTS (SELECT 1 FROM EventNames WHERE event_code = '_$Data$_.Delete')
    INSERT INTO EventNames (event_code, event_human_name) VALUES ('_$Data$_.Delete', 'Удаление');

IF NOT EXISTS (SELECT 1 FROM EventNames WHERE event_code = '_$Data$_.Post')
    INSERT INTO EventNames (event_code, event_human_name) VALUES ('_$Data$_.Post', 'Проведение');

IF NOT EXISTS (SELECT 1 FROM EventNames WHERE event_code = '_$Data$_.Unpost')
    INSERT INTO EventNames (event_code, event_human_name) VALUES ('_$Data$_.Unpost', 'Отмена проведения');

-- Транзакции
IF NOT EXISTS (SELECT 1 FROM EventNames WHERE event_code = '_$Transaction$_.Begin')
    INSERT INTO EventNames (event_code, event_human_name) VALUES ('_$Transaction$_.Begin', 'Транзакция. Начало');

IF NOT EXISTS (SELECT 1 FROM EventNames WHERE event_code = '_$Transaction$_.Commit')
    INSERT INTO EventNames (event_code, event_human_name) VALUES ('_$Transaction$_.Commit', 'Транзакция. Фиксация');

IF NOT EXISTS (SELECT 1 FROM EventNames WHERE event_code = '_$Transaction$_.Rollback')
    INSERT INTO EventNames (event_code, event_human_name) VALUES ('_$Transaction$_.Rollback', 'Транзакция. Отмена');

-- Сеансы
IF NOT EXISTS (SELECT 1 FROM EventNames WHERE event_code = '_$Session$_.Start')
    INSERT INTO EventNames (event_code, event_human_name) VALUES ('_$Session$_.Start', 'Сеанс. Начало');

IF NOT EXISTS (SELECT 1 FROM EventNames WHERE event_code = '_$Session$_.Finish')
    INSERT INTO EventNames (event_code, event_human_name) VALUES ('_$Session$_.Finish', 'Сеанс. Завершение');

IF NOT EXISTS (SELECT 1 FROM EventNames WHERE event_code = '_$Session$_.Authentication')
    INSERT INTO EventNames (event_code, event_human_name) VALUES ('_$Session$_.Authentication', 'Аутентификация');

IF NOT EXISTS (SELECT 1 FROM EventNames WHERE event_code = '_$Session$_.AuthenticationError')
    INSERT INTO EventNames (event_code, event_human_name) VALUES ('_$Session$_.AuthenticationError', 'Ошибка аутентификации');

-- Фоновые задания (Jobs)
IF NOT EXISTS (SELECT 1 FROM EventNames WHERE event_code = '_$Job$_.Start')
    INSERT INTO EventNames (event_code, event_human_name) VALUES ('_$Job$_.Start', 'Фоновое задание. Запуск');

IF NOT EXISTS (SELECT 1 FROM EventNames WHERE event_code = '_$Job$_.Succeed')
    INSERT INTO EventNames (event_code, event_human_name) VALUES ('_$Job$_.Succeed', 'Фоновое задание. Успешное завершение');

IF NOT EXISTS (SELECT 1 FROM EventNames WHERE event_code = '_$Job$_.Error')
    INSERT INTO EventNames (event_code, event_human_name) VALUES ('_$Job$_.Error', 'Фоновое задание. Ошибка выполнения');

IF NOT EXISTS (SELECT 1 FROM EventNames WHERE event_code = '_$Job$_.Terminate')
    INSERT INTO EventNames (event_code, event_human_name) VALUES ('_$Job$_.Terminate', 'Фоновое задание. Отмена');

-- Администрирование пользователей
IF NOT EXISTS (SELECT 1 FROM EventNames WHERE event_code = '_$User$_.New')
    INSERT INTO EventNames (event_code, event_human_name) VALUES ('_$User$_.New', 'Пользователь. Новый');

IF NOT EXISTS (SELECT 1 FROM EventNames WHERE event_code = '_$User$_.Update')
    INSERT INTO EventNames (event_code, event_human_name) VALUES ('_$User$_.Update', 'Пользователь. Изменение');

IF NOT EXISTS (SELECT 1 FROM EventNames WHERE event_code = '_$User$_.Delete')
    INSERT INTO EventNames (event_code, event_human_name) VALUES ('_$User$_.Delete', 'Пользователь. Удаление');

-- Конфигурация и Метаданные
IF NOT EXISTS (SELECT 1 FROM EventNames WHERE event_code = '_$Config$_.Update')
    INSERT INTO EventNames (event_code, event_human_name) VALUES ('_$Config$_.Update', 'Изменение конфигурации');

IF NOT EXISTS (SELECT 1 FROM EventNames WHERE event_code = '_$Metadata$_.Update')
    INSERT INTO EventNames (event_code, event_human_name) VALUES ('_$Metadata$_.Update', 'Изменение метаданных');

IF NOT EXISTS (SELECT 1 FROM EventNames WHERE event_code = '_$PerformError$_')
    INSERT INTO EventNames (event_code, event_human_name) VALUES ('_$PerformError$_', 'Ошибка выполнения');

IF NOT EXISTS (SELECT 1 FROM EventNames WHERE event_code = '_$InfoBase$_.EventLogSettingsUpdateError')
    INSERT INTO EventNames (event_code, event_human_name) VALUES ('_$InfoBase$_.EventLogSettingsUpdateError', 'Ошибка настройки журнала регистрации');

IF NOT EXISTS (SELECT 1 FROM EventNames WHERE event_code = '_$InfoBase$_.DBConfigUpdateError')
    INSERT INTO EventNames (event_code, event_human_name) VALUES ('_$InfoBase$_.DBConfigUpdateError', 'Ошибка обновления конфигурации БД');

-- Уровень важности
IF NOT EXISTS (SELECT 1 FROM SeverityLevels WHERE severity_id = 0)
    INSERT INTO SeverityLevels (severity_id, severity_name, severity_color)
    VALUES (0, 'Примечание', '#BDBDBD'); -- Серый

IF NOT EXISTS (SELECT 1 FROM SeverityLevels WHERE severity_id = 1)
    INSERT INTO SeverityLevels (severity_id, severity_name, severity_color)
    VALUES (1, 'Информация', '#2196F3'); -- Синий

IF NOT EXISTS (SELECT 1 FROM SeverityLevels WHERE severity_id = 2)
    INSERT INTO SeverityLevels (severity_id, severity_name, severity_color)
    VALUES (2, 'Предупреждение', '#FFC107'); -- Желтый

IF NOT EXISTS (SELECT 1 FROM SeverityLevels WHERE severity_id = 3)
    INSERT INTO SeverityLevels (severity_id, severity_name, severity_color)
    VALUES (3, 'Ошибка', '#F44336'); -- Красный