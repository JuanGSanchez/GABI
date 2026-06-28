package core.reports;

import java.util.List;

/**
 * Renders a {@link Report} to CSV (RFC 4180) or JSON (SPEC-21). Pure and dependency-free.
 *
 * @author GABI SDD pipeline (SPEC-21 reports and data export)
 */
public final class ReportExporter {

    private ReportExporter() {
    }

    /** RFC 4180 CSV: header row then data rows, with quote/comma/newline escaping. */
    public static String toCsv(Report report) {
        StringBuilder sb = new StringBuilder();
        sb.append(csvRow(report.headers()));
        for (List<String> row : report.rows()) {
            sb.append('\n').append(csvRow(row));
        }
        return sb.toString();
    }

    /** JSON: an array of objects keyed by header. Values are emitted as JSON strings. */
    public static String toJson(Report report) {
        List<String> headers = report.headers();
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        List<List<String>> rows = report.rows();
        for (int r = 0; r < rows.size(); r++) {
            if (r > 0) {
                sb.append(',');
            }
            sb.append('{');
            List<String> row = rows.get(r);
            for (int c = 0; c < headers.size(); c++) {
                if (c > 0) {
                    sb.append(',');
                }
                sb.append(jsonString(headers.get(c))).append(':')
                        .append(jsonString(c < row.size() ? row.get(c) : ""));
            }
            sb.append('}');
        }
        sb.append(']');
        return sb.toString();
    }

    private static String csvRow(List<String> cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(csvCell(cells.get(i)));
        }
        return sb.toString();
    }

    private static String csvCell(String value) {
        String v = value == null ? "" : value;
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            return '"' + v.replace("\"", "\"\"") + '"';
        }
        return v;
    }

    private static String jsonString(String value) {
        String v = value == null ? "" : value;
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < v.length(); i++) {
            char ch = v.charAt(i);
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(ch);
            }
        }
        return sb.append('"').toString();
    }
}
