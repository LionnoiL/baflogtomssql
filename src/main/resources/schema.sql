IF
OBJECT_ID('SeverityLevels', 'U') IS NULL
CREATE TABLE SeverityLevels
(
    severity_id    INT PRIMARY KEY,
    severity_name  NVARCHAR(50) NOT NULL,
    severity_color VARCHAR(20)
) GO

IF
OBJECT_ID('Users', 'U') IS NULL
CREATE TABLE Users
(
    user_id   INT PRIMARY KEY,
    user_name NVARCHAR(255) NOT NULL,
    uuid      VARCHAR(36) UNIQUE NOT NULL
) GO

IF
OBJECT_ID('Computers', 'U') IS NULL
CREATE TABLE Computers
(
    computer_id   INT PRIMARY KEY,
    computer_name NVARCHAR(255) UNIQUE NOT NULL
) GO

IF
OBJECT_ID('Applications', 'U') IS NULL
CREATE TABLE Applications
(
    app_id   INT PRIMARY KEY,
    app_name NVARCHAR(255) UNIQUE NOT NULL
) GO

IF
OBJECT_ID('Metadata', 'U') IS NULL
CREATE TABLE Metadata
(
    metadata_id   INT PRIMARY KEY,
    metadata_name NVARCHAR(255) NOT NULL,
    uuid          VARCHAR(36) UNIQUE NOT NULL
) GO

IF
OBJECT_ID('EventNames', 'U') IS NULL
CREATE TABLE EventNames
(
    event_id         INT PRIMARY KEY,
    event_code       VARCHAR(255) UNIQUE NOT NULL,
    event_human_name NVARCHAR(255)
) GO

IF OBJECT_ID('Settings', 'U') IS NULL
CREATE TABLE Settings
(
    setting_key   NVARCHAR(100) PRIMARY KEY,
    setting_value NVARCHAR(MAX)
) GO

IF
OBJECT_ID('EventLogSync', 'U') IS NULL
CREATE TABLE EventLogSync
(
    row_id           BIGINT,
    event_date       DATETIME2(3) NOT NULL,
    transaction_date DATETIME2(3) NOT NULL,
    transaction_id   INT,
    session_id       INT,
    user_id          INT,
    event_id         INT,
    computer_id      INT,
    app_id           INT,
    metadata_id      INT,
    severity         INT,
    comment          NVARCHAR(4000),
    data_info        NVARCHAR(1000),
    data             NVARCHAR(4000),
    search_content   NVARCHAR(MAX),
    CONSTRAINT PK_EventLogSync PRIMARY KEY CLUSTERED (row_id),

    CONSTRAINT FK_EventLog_User FOREIGN KEY (user_id) REFERENCES Users (user_id),
    CONSTRAINT FK_EventLog_Event FOREIGN KEY (event_id) REFERENCES EventNames (event_id),
    CONSTRAINT FK_EventLog_Computer FOREIGN KEY (computer_id) REFERENCES Computers (computer_id),
    CONSTRAINT FK_EventLog_App FOREIGN KEY (app_id) REFERENCES Applications (app_id),
    CONSTRAINT FK_EventLog_Metadata FOREIGN KEY (metadata_id) REFERENCES Metadata (metadata_id),
    CONSTRAINT FK_EventLog_Severity FOREIGN KEY (severity) REFERENCES SeverityLevels (severity_id)
) WITH (DATA_COMPRESSION = PAGE)
    GO
    IF NOT EXISTS
(
    SELECT
    *
    FROM
    sys
    .
    columns
    WHERE
    object_id =
    OBJECT_ID
(
    'EventLogSync'
)
    AND name = 'event_hour')
BEGIN
ALTER TABLE EventLogSync
    ADD event_hour AS DATEPART(HOUR, event_date) PERSISTED;
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns
                   WHERE object_id = OBJECT_ID('EventLogSync')
                   AND name = 'event_dw')
BEGIN
ALTER TABLE EventLogSync
    ADD event_dw AS (DATEDIFF(day, 0, event_date) % 7) + 1 PERSISTED;
END
GO

IF OBJECT_ID('ViewEventLog', 'V') IS NOT NULL
DROP VIEW ViewEventLog
    GO

CREATE VIEW ViewEventLog AS
SELECT l.row_id,
       l.event_date,
       l.event_hour,
       l.event_dw,
       l.transaction_date,
       l.transaction_id,
       l.session_id,
       u.user_name,
       u.uuid     AS user_uuid,
       en.event_human_name,
       en.event_code,
       c.computer_name,
       a.app_name,
       m.metadata_name,
       m.uuid     AS metadata_uuid,
       sl.severity_name,
       sl.severity_color,
       l.comment,
       l.data_info,
       l.data,
       l.search_content,
       l.user_id,
       l.event_id,
       l.computer_id,
       l.app_id,
       l.metadata_id,
       l.severity AS severity_id
FROM EventLogSync l
         LEFT JOIN Users u ON l.user_id = u.user_id
         LEFT JOIN EventNames en ON l.event_id = en.event_id
         LEFT JOIN Computers c ON l.computer_id = c.computer_id
         LEFT JOIN Applications a ON l.app_id = a.app_id
         LEFT JOIN Metadata m ON l.metadata_id = m.metadata_id
         LEFT JOIN SeverityLevels sl ON l.severity = sl.severity_id GO

IF NOT EXISTS (SELECT * FROM sys.indexes 
               WHERE name = 'IX_EventLogSync_EventDate' 
               AND object_id = OBJECT_ID('EventLogSync'))
BEGIN
CREATE INDEX IX_EventLogSync_EventDate
    ON EventLogSync (event_date DESC) INCLUDE (user_id, event_id, severity, computer_id, app_id, metadata_id)
        WITH (DATA_COMPRESSION = PAGE);
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes 
               WHERE name = 'IX_EventLogSync_EventId_Date_Session' 
               AND object_id = OBJECT_ID('EventLogSync'))
BEGIN
CREATE INDEX IX_EventLogSync_EventId_Date_Session
    ON EventLogSync (event_id, event_date) INCLUDE (session_id, metadata_id)
        WITH (DATA_COMPRESSION = PAGE);
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes
               WHERE name = 'IX_EventLogSync_EventId_Date_Session'
               AND object_id = OBJECT_ID('EventLogSync'))
BEGIN
CREATE INDEX IX_EventLogSync_EventId_Date_Session
    ON EventLogSync (event_id, event_date) INCLUDE (session_id, metadata_id)
        WITH (DATA_COMPRESSION = PAGE);
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes 
               WHERE name = 'IX_EventLogSync_Date_Severity' 
               AND object_id = OBJECT_ID('EventLogSync'))
BEGIN
CREATE INDEX IX_EventLogSync_Date_Severity
    ON EventLogSync (event_date, severity) INCLUDE (user_id, event_id)
        WITH (DATA_COMPRESSION = PAGE);
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes
               WHERE name = 'IX_EventLogSync_Date_Only'
               AND object_id = OBJECT_ID('EventLogSync'))
BEGIN
CREATE INDEX IX_EventLogSync_Date_Only
    ON EventLogSync (event_date) WITH (DATA_COMPRESSION = PAGE);
END
GO

    IF NOT EXISTS (SELECT * FROM sys.indexes 
                   WHERE name = 'IX_EventLogSync_Date_Hour' 
                   AND object_id = OBJECT_ID('EventLogSync'))
BEGIN
CREATE INDEX IX_EventLogSync_Date_Hour
    ON EventLogSync (event_date, event_hour) WITH (DATA_COMPRESSION = PAGE);
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes
                   WHERE name = 'IX_EventLogSync_Date_Hour_DW'
                   AND object_id = OBJECT_ID('EventLogSync'))
BEGIN
CREATE INDEX IX_EventLogSync_Date_Hour_DW
    ON EventLogSync (event_date, event_hour, event_dw)
    WITH (DATA_COMPRESSION = PAGE);
END
GO