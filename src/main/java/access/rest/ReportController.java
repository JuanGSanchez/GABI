package access.rest;

import core.reports.Report;
import core.reports.ReportExporter;
import core.reports.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Read-only report/export endpoints (SPEC-21).
 *
 * <ul>
 *   <li>{@code GET /api/reports} — the available report ids.</li>
 *   <li>{@code GET /api/reports/{name}?format=csv|json} — download a report.</li>
 * </ul>
 *
 * <p>Reports project only catalogue/circulation data — never DB users or credentials — so
 * the export surface stays read-only and secret-free. An unknown report id yields 404.
 *
 * @author GABI SDD pipeline (SPEC-21 reports and data export)
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final MediaType CSV = new MediaType("text", "csv");

    private final ReportService reports;

    public ReportController(ReportService reports) {
        this.reports = reports;
    }

    /** Lists the available report ids. */
    @GetMapping
    public List<String> available() {
        return reports.available();
    }

    /** Downloads a report as CSV or JSON (default JSON). */
    @GetMapping("/{name}")
    public ResponseEntity<String> export(
            @PathVariable String name,
            @RequestParam(defaultValue = "json") String format) {

        Report report;
        try {
            report = reports.byName(name);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown report: " + name);
        }

        boolean csv = "csv".equalsIgnoreCase(format);
        String body = csv ? ReportExporter.toCsv(report) : ReportExporter.toJson(report);
        String filename = report.name() + (csv ? ".csv" : ".json");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(csv ? CSV : MediaType.APPLICATION_JSON)
                .body(body);
    }
}
