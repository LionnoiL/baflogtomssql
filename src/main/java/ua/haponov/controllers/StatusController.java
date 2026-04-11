package ua.haponov.controllers;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.haponov.LogMemoryAppender;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/status")
@RequiredArgsConstructor
public class StatusController {

    private final LogMemoryAppender logAppender;

    @GetMapping
    public ServiceStatus getStatus() {
        return ServiceStatus.builder()
                .status("UP")
                .timestamp(LocalDateTime.now())
                .serviceName("baf-log-to-mssql")
                .recentLogs(logAppender.getRecentLogs())
                .build();
    }

    @Data
    @Builder
    public static class ServiceStatus {
        private String status;
        private LocalDateTime timestamp;
        private String serviceName;
        private List<LogMemoryAppender.AppLogEntry> recentLogs;
    }
}
