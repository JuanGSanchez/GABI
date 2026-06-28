package core.reports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ui.desktop.FakeLibraryService;

import static org.assertj.core.api.Assertions.*;

/**
 * SPEC-21: report builders project catalogue/circulation data correctly and never expose
 * DB users or secrets.
 */
class ReportServiceTest {

    private FakeLibraryService library;
    private ReportServiceImpl reports;

    @BeforeEach
    void setUp() {
        library = new FakeLibraryService();
        reports = new ReportServiceImpl(library);
    }

    @Test
    @DisplayName("catalogue report lists books with status")
    void catalogue() {
        library.addBook(1, "Dune", "Herbert");
        library.addBook(2, "It", "King");
        Report r = reports.catalogue();
        assertThat(r.headers()).containsExactly("id", "title", "author", "status");
        assertThat(r.size()).isEqualTo(2);
        assertThat(r.rows().get(0)).containsExactly("1", "Dune", "Herbert", "available");
    }

    @Test
    @DisplayName("loans-per-member counts active loans per member")
    void loansPerMember() {
        library.addMember(1, "Alice", "Smith");
        library.addMember(2, "Bob", "Jones");
        library.addBook(1, "Dune", "Herbert");
        library.addBook(2, "It", "King");
        library.createLoan(1, 1, 1);
        library.createLoan(2, 1, 2);
        Report r = reports.loansPerMember();
        assertThat(r.headers()).containsExactly("memberId", "activeLoans");
        assertThat(r.rows()).contains(java.util.List.of("1", "2"), java.util.List.of("2", "0"));
    }

    @Test
    @DisplayName("active loans report carries a due-date column")
    void activeLoans() {
        library.addMember(1, "Alice", "Smith");
        library.addBook(1, "Dune", "Herbert");
        library.createLoan(1, 1, 1);
        Report r = reports.activeLoans();
        assertThat(r.headers()).contains("dueDate");
        assertThat(r.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("available() lists every report and byName dispatches; unknown throws")
    void availableAndDispatch() {
        assertThat(reports.available())
                .containsExactly("catalogue", "active-loans", "overdue-loans", "loans-per-member");
        assertThat(reports.byName("catalogue").name()).isEqualTo("catalogue");
        assertThatThrownBy(() -> reports.byName("users"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("no report exposes a user/password/credential column")
    void noSecretColumns() {
        library.addUser(1, "admin2", new char[]{'x'});
        for (String name : reports.available()) {
            String csv = ReportExporter.toCsv(reports.byName(name)).toLowerCase();
            assertThat(csv).doesNotContain("password").doesNotContain("secret").doesNotContain("api-key");
        }
    }
}
