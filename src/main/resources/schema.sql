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
    uuid VARCHAR(36) UNIQUE NOT NULL
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
    uuid VARCHAR(36) UNIQUE NOT NULL
);

IF
OBJECT_ID('EventNames', 'U') IS NULL
CREATE TABLE EventNames
(
    event_id         INT PRIMARY KEY,
    event_code       VARCHAR(255) UNIQUE NOT NULL,
    event_human_name NVARCHAR(255)
);

IF
OBJECT_ID('SeverityLevels', 'U') IS NULL
CREATE TABLE SeverityLevels
(
    severity_id    INT PRIMARY KEY, -- 0, 1, 2, 3
    severity_name  NVARCHAR(50) NOT NULL,
    severity_color VARCHAR(20)
);

IF
OBJECT_ID('EventLogSync', 'U') IS NULL
CREATE TABLE EventLogSync
(
    row_id      BIGINT PRIMARY KEY,
    event_date  DATETIME2(3) NOT NULL,
    user_id     INT,
    event_id    INT,
    computer_id INT,
    app_id      INT,
    metadata_id INT,
    severity    INT,
    comment     NVARCHAR(4000),
    data_info   NVARCHAR(1000)
)
    WITH (DATA_COMPRESSION = PAGE);