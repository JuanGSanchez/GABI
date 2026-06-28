package core.reports;

import java.util.List;

/**
 * A tabular report (SPEC-21): a name, ordered column headers, and string rows.
 *
 * <p>Locale-free and provider-free; it is a pure projection of catalogue data built by
 * {@link ReportService} and rendered by {@link ReportExporter}. It deliberately models
 * only catalogue/circulation data — never DB users, credentials, or secrets — so any
 * export is safe to hand out.
 *
 * @param name    a stable report id (e.g. "catalogue", "overdue-loans")
 * @param headers the column headers, in order
 * @param rows    the data rows; each row has one cell per header
 * @author GABI SDD pipeline (SPEC-21 reports and data export)
 */
public record Report(String name, List<String> headers, List<List<String>> rows) {

    public Report {
        headers = List.copyOf(headers);
        rows = rows.stream().map(List::copyOf).toList();
    }

    /** Number of data rows (excluding the header). */
    public int size() {
        return rows.size();
    }
}
