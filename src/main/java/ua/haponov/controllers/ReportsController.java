package ua.haponov.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ua.haponov.services.ReportService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportsController {

    private final ReportService reportService;

    @GetMapping("/fragment/{type}")
    public String getReportFragment(
            @PathVariable String type,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            Model model) {

        if (from == null || from.isEmpty()) {
            from = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        }
        if (to == null || to.isEmpty()) {
            to = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        }

        model.addAttribute("from", from);
        model.addAttribute("to", to);

        Object data = switch (type) {
            case "top-errors" -> reportService.getMainDashboard(from, to);
            case "integrations" -> reportService.getBackgroundTasks(from, to);
            case "current-users" -> reportService.getCurrentUsers();
            default -> null;
        };
        model.addAttribute("reportData", data);

        return "reports/" + type + " :: report";
    }
}
