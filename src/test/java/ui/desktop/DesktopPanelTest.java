package ui.desktop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ui.UiText;

import java.awt.GraphicsEnvironment;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * SPEC-18 acceptance: the desktop adapter performs the console operations (add / list /
 * search / delete) over the shared core, with no business logic in the UI. The Swing
 * widget construction is skipped in a headless CI; the catalogue behavior is still
 * asserted through the panel's model.
 */
class DesktopPanelTest {

    private FakeLibraryService service;
    private UiText text;

    @BeforeEach
    void setUp() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Swing panels require a display");
        service = new FakeLibraryService();
        text = UiText.english();
    }

    @Test
    @DisplayName("BooksPanel reflects books added through the core service")
    void booksPanelReflectsCore() {
        service.addBook(1, "Dune", "Herbert");
        service.addBook(2, "It", "King");
        BooksPanel panel = new BooksPanel(service, text);

        assertThat(panel.model.getRowCount()).isEqualTo(2);
        assertThat(panel.model.getValueAt(0, 1)).isEqualTo("Dune");

        // delete through the core, refresh, view updates
        service.deleteBook(1);
        panel.refresh();
        assertThat(panel.model.getRowCount()).isEqualTo(1);
        assertThat(panel.model.getValueAt(0, 1)).isEqualTo("It");
    }

    @Test
    @DisplayName("MembersPanel and LoansPanel render the live catalogue")
    void membersAndLoans() {
        service.addMember(1, "Alice", "Smith");
        service.addBook(1, "Dune", "Herbert");
        service.createLoan(1, 1, 1);

        MembersPanel members = new MembersPanel(service, text);
        LoansPanel loans = new LoansPanel(service, text);

        assertThat(members.model.getRowCount()).isEqualTo(1);
        assertThat(members.model.getValueAt(0, 1)).isEqualTo("Alice");
        assertThat(loans.model.getRowCount()).isEqualTo(1);
        assertThat(loans.model.getValueAt(0, 2)).isEqualTo(1); // book id column
    }

    @Test
    @DisplayName("UsersPanel lists users without exposing a password column")
    void usersPanelNoPasswordColumn() {
        service.addUser(1, "admin2", new char[]{'x'});
        UsersPanel panel = new UsersPanel(service, text);
        assertThat(panel.model.getRowCount()).isEqualTo(1);
        // only ID + name columns are exposed; no password/secret column on the surface
        assertThat(panel.model.getColumnCount()).isEqualTo(2);
        assertThat(panel.model.getValueAt(0, 1)).isEqualTo("admin2");
    }

    @Test
    @DisplayName("MainFrame hosts one tab per entity")
    void mainFrameTabs() {
        MainFrame frame = new MainFrame(service, text);
        try {
            assertThat(frame.getTabs().getTabCount()).isEqualTo(4);
            assertThat(frame.getBooksPanel()).isNotNull();
            assertThat(frame.getUsersPanel()).isNotNull();
        } finally {
            frame.dispose();
        }
    }
}
