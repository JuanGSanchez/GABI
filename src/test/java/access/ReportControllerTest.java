package access;

import access.rest.ReportController;
import core.reports.ReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import ui.desktop.FakeLibraryService;

import static org.assertj.core.api.Assertions.*;

/**
 * SPEC-21: the report endpoint returns CSV and JSON downloads and 404s an unknown report.
 * Tested as a plain object (no Spring context), matching the access-layer test style.
 */
class ReportControllerTest {

    private ReportController controller;

    @BeforeEach
    void setUp() {
        FakeLibraryService library = new FakeLibraryService();
        library.addBook(1, "Dune", "Herbert");
        controller = new ReportController(new ReportServiceImpl(library));
    }

    @Test
    @DisplayName("lists available reports")
    void available() {
        assertThat(controller.available()).contains("catalogue", "overdue-loans");
    }

    @Test
    @DisplayName("JSON export returns application/json with a content-disposition")
    void json() {
        ResponseEntity<String> resp = controller.export("catalogue", "json");
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(resp.getBody()).startsWith("[").contains("Dune");
        assertThat(resp.getHeaders().getFirst("Content-Disposition")).contains("catalogue.json");
    }

    @Test
    @DisplayName("CSV export returns text/csv")
    void csv() {
        ResponseEntity<String> resp = controller.export("catalogue", "csv");
        assertThat(resp.getHeaders().getContentType().toString()).isEqualTo("text/csv");
        assertThat(resp.getBody()).startsWith("id,title,author,status");
    }

    @Test
    @DisplayName("an unknown report yields 404")
    void unknown404() {
        assertThatThrownBy(() -> controller.export("nope", "json"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }
}
