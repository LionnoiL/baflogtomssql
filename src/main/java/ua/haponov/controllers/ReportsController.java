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
            from = switch (type) {
                case "load-graph", "suspicious", "administrative-action" ->
                        LocalDate.now().minusDays(7).format(DateTimeFormatter.ISO_DATE);
                case "top-errors", "intra-diurnal-activity", "activity_matrix" ->
                        LocalDate.now().minusDays(30).format(DateTimeFormatter.ISO_DATE);
                default -> LocalDate.now().format(DateTimeFormatter.ISO_DATE);
            };
        }

        if (to == null || to.isEmpty()) {
            to = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        }

        Object data = switch (type) {
            case "top-errors" -> reportService.getMainDashboard(from, to);
            case "integrations" -> reportService.getBackgroundTasks(from, to);
            case "current-users" -> reportService.getCurrentUsers();
            case "suspicious" -> reportService.getSuspicionsReasons(from, to);
            case "load-graph" -> reportService.getLoadGraph(from, to);
            case "intra-diurnal-activity" -> reportService.getIntraDiurnalActivity(from, to);
            case "activity_matrix" -> reportService.getActivityMatrix(from, to);
            case "auth-audit" -> reportService.getAuthorizationAudit(from, to);
            case "administrative-action" -> reportService.getAdministrativeActions(from, to);
            default -> null;
        };

        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("reportData", data);

        return "reports/" + type + " :: report";
    }
}
