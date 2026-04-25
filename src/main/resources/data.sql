IF NOT EXISTS (SELECT 1 FROM SeverityLevels WHERE severity_id = 0)
    INSERT INTO SeverityLevels (severity_id, severity_name, severity_color)
    VALUES (0, 'Примечание', '#BDBDBD')
GO

IF NOT EXISTS (SELECT 1 FROM SeverityLevels WHERE severity_id = 1)
    INSERT INTO SeverityLevels (severity_id, severity_name, severity_color)
    VALUES (1, 'Информация', '#2196F3')
GO

IF NOT EXISTS (SELECT 1 FROM SeverityLevels WHERE severity_id = 2)
    INSERT INTO SeverityLevels (severity_id, severity_name, severity_color)
    VALUES (2, 'Предупреждение', '#FFC107')
GO

IF NOT EXISTS (SELECT 1 FROM SeverityLevels WHERE severity_id = 3)
    INSERT INTO SeverityLevels (severity_id, severity_name, severity_color)
    VALUES (3, 'Ошибка', '#F44336')
GO

INSERT INTO Settings (setting_key, setting_value)
SELECT 'cleanup_enabled', 'false'
    WHERE NOT EXISTS (SELECT 1 FROM Settings WHERE setting_key = 'cleanup_enabled')
GO

INSERT INTO Settings (setting_key, setting_value)
SELECT 'cleanup_excluded_event_ids', ''
    WHERE NOT EXISTS (SELECT 1 FROM Settings WHERE setting_key = 'cleanup_excluded_event_ids')
GO

INSERT INTO Settings (setting_key, setting_value)
SELECT 'cleanup_days', '365'
    WHERE NOT EXISTS (SELECT 1 FROM Settings WHERE setting_key = 'cleanup_days')
GO