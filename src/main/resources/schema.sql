IF
OBJECT_ID('EventNames', 'U') IS NULL
CREATE TABLE EventNames
(
    event_code       NVARCHAR(255) PRIMARY KEY,
    event_human_name NVARCHAR(255) NOT NULL
);

IF
OBJECT_ID('SeverityLevels', 'U') IS NULL
CREATE TABLE SeverityLevels
(
    severity_id    INT PRIMARY KEY,
    severity_name  NVARCHAR(50) NOT NULL,
    severity_color NVARCHAR(20)
);