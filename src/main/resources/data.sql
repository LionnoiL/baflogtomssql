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