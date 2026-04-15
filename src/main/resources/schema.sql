IF
OBJECT_ID('SeverityLevels', 'U') IS NULL
CREATE TABLE SeverityLevels
(
    severity_id    INT PRIMARY KEY,
    severity_name  NVARCHAR(50) NOT NULL,
    severity_color VARCHAR(20)
);

IF
OBJECT_ID('Users', 'U') IS NULL
CREATE TABLE Users
(
    user_id   INT PRIMARY KEY,
    user_name NVARCHAR(255) NOT NULL,
    uuid      VARCHAR(36) UNIQUE NOT NULL
);

IF
OBJECT_ID('Computers', 'U') IS NULL
CREATE TABLE Computers
(
    computer_id   INT PRIMARY KEY,
    computer_name NVARCHAR(255) UNIQUE NOT NULL
);

IF
OBJECT_ID('Applications', 'U') IS NULL
CREATE TABLE Applications
(
    app_id   INT PRIMARY KEY,
    app_name NVARCHAR(255) UNIQUE NOT NULL
);

IF
OBJECT_ID('Metadata', 'U') IS NULL
CREATE TABLE Metadata
(
    metadata_id   INT PRIMARY KEY,
    metadata_name NVARCHAR(255) NOT NULL,
    uuid          VARCHAR(36) UNIQUE NOT NULL
);

IF
OBJECT_ID('EventNames', 'U') IS NULL
CREATE TABLE EventNames
(
    event_id         INT PRIMARY KEY,
    event_code       VARCHAR(255) UNIQUE NOT NULL,
    event_human_name NVARCHAR(255)
);

IF OBJECT_ID('Settings', 'U') IS NULL
CREATE TABLE Settings
(
    setting_key   NVARCHAR(100) PRIMARY KEY,
    setting_value NVARCHAR(MAX)
);

IF
OBJECT_ID('EventLogSync', 'U') IS NULL
CREATE TABLE EventLogSync
(
    row_id         BIGINT,
    event_date     DATETIME2(3) NOT NULL,
    transaction_date     DATETIME2(3) NOT NULL,
    transaction_id        INT,
    session_id        INT,
    user_id        INT,
    event_id       INT,
    computer_id    INT,
    app_id         INT,
    metadata_id    INT,
    severity       INT,
    comment        NVARCHAR(4000),
    data_info      NVARCHAR(1000),
    data           NVARCHAR(4000),
    search_content NVARCHAR(MAX),
    CONSTRAINT PK_EventLogSync PRIMARY KEY CLUSTERED (row_id),

    CONSTRAINT FK_EventLog_User FOREIGN KEY (user_id) REFERENCES Users (user_id),
    CONSTRAINT FK_EventLog_Event FOREIGN KEY (event_id) REFERENCES EventNames (event_id),
    CONSTRAINT FK_EventLog_Computer FOREIGN KEY (computer_id) REFERENCES Computers (computer_id),
    CONSTRAINT FK_EventLog_App FOREIGN KEY (app_id) REFERENCES Applications (app_id),
    CONSTRAINT FK_EventLog_Metadata FOREIGN KEY (metadata_id) REFERENCES Metadata (metadata_id),
    CONSTRAINT FK_EventLog_Severity FOREIGN KEY (severity) REFERENCES SeverityLevels (severity_id)
)
    WITH (DATA_COMPRESSION = PAGE);
