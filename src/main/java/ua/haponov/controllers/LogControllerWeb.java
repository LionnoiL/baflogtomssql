package ua.haponov.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ua.haponov.dto.*;
import ua.haponov.services.DictionaryService;
import ua.haponov.services.LogService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/logs")
@RequiredArgsConstructor
public class LogControllerWeb {

    private final LogService logService;
    private final DictionaryService dictionaryService;

    @GetMapping
    public String showLogsPage(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "50") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) List<Integer> levels,
            @RequestParam(required = false) List<Integer> sources,
            @RequestParam(required = false) List<Integer> users,
            @RequestParam(required = false) List<Integer> computers,
            @RequestParam(required = false) List<Integer> events,
            @RequestParam(required = false) List<Integer> metadata,
            Model model) {

        List<SeverityLevel> severityLevels = dictionaryService.getSeverityLevels();
        List<Application> appSources = dictionaryService.getApplications();
        List<Computer> computerSources = dictionaryService.getComputers();
        List<Event> eventSources = dictionaryService.getEventNames();
        List<Metadata> metadataSources = dictionaryService.getMetadata();
        List<User> userSources = dictionaryService.getUsers();

        String guidSearch = isGuid(search) ? search : null;
        String textSearch = !isGuid(search) ? search : null;

        Map<String, Object> logsData = logService.getLogsPaged(page, size, guidSearch, textSearch, from, to, computers, users, sources, events, levels, metadata);

        model.addAttribute("severityLevels", severityLevels);
        model.addAttribute("appSources", appSources);
        model.addAttribute("computerSources", computerSources);
        model.addAttribute("eventSources", eventSources);
        model.addAttribute("metadataSources", metadataSources);
        model.addAttribute("userSources", userSources);
        model.addAttribute("logs", logsData.get("content"));
        model.addAttribute("search", search);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("levels", levels);
        model.addAttribute("sources", sources);
        model.addAttribute("users", users);
        model.addAttribute("computers", computers);
        model.addAttribute("events", events);
        model.addAttribute("metadata", metadata);

        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalPages", logsData.get("totalPages"));

        return "logs";
    }

    private boolean isGuid(String search) {
        if (search == null || search.isBlank()) {
            return false;
        }
        try {
            java.util.UUID.fromString(search);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
