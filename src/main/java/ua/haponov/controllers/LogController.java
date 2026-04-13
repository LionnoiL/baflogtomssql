package ua.haponov.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.haponov.services.LogService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @GetMapping
    public Map<String, Object> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) List<Integer> computerIds,
            @RequestParam(required = false) List<Integer> userIds,
            @RequestParam(required = false) List<Integer> appIds,
            @RequestParam(required = false) List<Integer> eventIds,
            @RequestParam(required = false) List<Integer> severityIds,
            @RequestParam(required = false) List<Integer> metadataIds) {

        return logService.getLogsPaged(page, size, search, from, to,
                computerIds, userIds, appIds, eventIds, severityIds, metadataIds);
    }
}
