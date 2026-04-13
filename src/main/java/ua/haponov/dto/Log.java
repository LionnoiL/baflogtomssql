package ua.haponov.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Log {

    private Long id;
    private LocalDateTime timestamp;
    private String severityName;
    private String severityColor;
    private String message;
    private String metadataName;
    private String applicationName;
    private String computerName;
    private String eventName;
    private String userName;

}