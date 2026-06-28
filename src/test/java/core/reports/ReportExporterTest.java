package core.reports;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * SPEC-21: CSV (RFC 4180 escaping) and JSON rendering of a report.
 */
class ReportExporterTest {

    private Report sample() {
        return new Report("t",
                List.of("id", "title"),
                List.of(
                        List.of("1", "Plain"),
                        List.of("2", "Has, comma"),
                        List.of("3", "Has \"quote\"")));
    }

    @Test
    @DisplayName("CSV emits a header row and escapes commas and quotes")
    void csv() {
        String csv = ReportExporter.toCsv(sample());
        String[] lines = csv.split("\n");
        assertThat(lines[0]).isEqualTo("id,title");
        assertThat(lines[1]).isEqualTo("1,Plain");
        assertThat(lines[2]).isEqualTo("2,\"Has, comma\"");
        assertThat(lines[3]).isEqualTo("3,\"Has \"\"quote\"\"\"");
    }

    @Test
    @DisplayName("JSON emits an array of header-keyed objects with escaping")
    void json() {
        String json = ReportExporter.toJson(sample());
        assertThat(json).startsWith("[{").endsWith("}]");
        assertThat(json).contains("\"id\":\"1\"").contains("\"title\":\"Plain\"");
        assertThat(json).contains("\"title\":\"Has, comma\"");
        assertThat(json).contains("\\\"quote\\\"");
    }

    @Test
    @DisplayName("an empty report renders an empty CSV body and empty JSON array")
    void empty() {
        Report empty = new Report("e", List.of("a", "b"), List.of());
        assertThat(ReportExporter.toCsv(empty)).isEqualTo("a,b");
        assertThat(ReportExporter.toJson(empty)).isEqualTo("[]");
    }
}
